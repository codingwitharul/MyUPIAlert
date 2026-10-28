package com.codingwitharul.myupialert

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.util.Log
import com.codingwitharul.myupialert.dedup.DedupAction
import com.codingwitharul.myupialert.dedup.PaymentDeduplicator
import com.codingwitharul.myupialert.fraud.FraudDetector
import com.codingwitharul.myupialert.fraud.FraudType
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.widget.HeatmapWidgetProvider
import com.codingwitharul.myupialert.repository.TransactionRepository
import com.codingwitharul.myupialert.smsparser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.UUID
import java.util.concurrent.Executors

class NotificationListener : NotificationListenerService() {
    private val transactionRepository: TransactionRepository by inject()
    private val smsParser: SmsParser by inject()
    private val paymentDeduplicator: PaymentDeduplicator by inject()
    private val fraudDetector: FraudDetector by inject()

    private val executor = Executors.newSingleThreadExecutor()

    private fun isUpiApplication(packageName: String?): Boolean {
        if (packageName == null) return false
        for (pkg in RECOGNIZED_UPI_PACKAGES) {
            if (packageName.contains(pkg)) {
                return true
            }
        }
        try {
            val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
            val apps = packageManager.queryIntentActivities(upiIntent, 0)
            for (info in apps) {
                if (info.activityInfo != null && packageName == info.activityInfo.packageName) {
                    return true
                }
            }
        } catch (ignored: Exception) {}
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return
        if (!isUpiApplication(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val titleSeq = extras.getCharSequence(Notification.EXTRA_TITLE)
        val textSeq = extras.getCharSequence(Notification.EXTRA_TEXT)
        val title = titleSeq?.toString() ?: ""
        val text = textSeq?.toString() ?: ""

        val messageBody = ("$title $text").trim()
        if (messageBody.isEmpty()) return

        val isCredit = smsParser.isCreditTransaction(messageBody)
        val isDebit = smsParser.isDebitTransaction(messageBody)

        // Ignore notifications that are neither credit nor debit transactions
        if (!isCredit && !isDebit) {
            return
        }

        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE)

        val amountStr = smsParser.extractAmount(messageBody)
        val senderName = smsParser.getSenderName(messageBody)
        val utr = smsParser.extractReferenceNumber(messageBody)

        // Deduplication & Anti-Fraud evaluation
        val decision = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            paymentDeduplicator.evaluate(
                amountStr = amountStr,
                senderName = senderName,
                utr = utr,
                source = "notification",
                rawBody = messageBody
            )
        }

        when (decision.action) {
            DedupAction.SUPPRESS_TWIN -> {
                // Silently drop twin notification (e.g. Bank SMS arrived first, or notification was re-posted)
                Log.d(TAG, "Suppressed twin notification from $packageName for ₹$amountStr (${decision.reason})")
                if (utr != null && decision.existingTransaction != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        paymentDeduplicator.enrichExistingTransactionIfPossible(decision.existingTransaction, utr, packageName)
                    }
                }
                return
            }
            DedupAction.WARN_FRAUD_REPLAY -> {
                // Anti-fraud: Replay of an OLD transaction with matching UTR (> 3 minutes ago)
                Log.w(TAG, "Duplicate replay alert: ₹$amountStr from $packageName with UTR $utr")
                val warnText = "Warning! Duplicate payment alert detected for rupees $amountStr"
                speakTts(appContext, warnText)
                return
            }
            DedupAction.PROCESS_NEW -> {
                // Legitimate new payment
            }
        }

        val transactionId = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val transaction = PaymentTransaction(
            id = transactionId,
            amount = amountStr,
            type = if (isCredit) "credit" else "debit",
            sender = packageName,
            body = messageBody,
            timestamp = System.currentTimeMillis()
        )

        paymentDeduplicator.recordProcessed(
            amountStr = amountStr,
            senderName = senderName,
            utr = utr,
            source = "notification",
            rawBody = messageBody,
            transactionId = transactionId
        )

        executor.execute {
            try {
                CoroutineScope(Dispatchers.IO).launch { transactionRepository.insert(transaction) }
                val intent = Intent("com.codingwitharul.myupialert.PAYMENT_RECEIVED").apply {
                    `package` = appContext.packageName
                }
                appContext.sendBroadcast(intent)

                // Refresh home screen widget with new data
                HeatmapWidgetProvider.refreshAll(appContext)

                // Refresh sticky status bar notification
                ForegroundTtsService.updateNotification(appContext)
            } catch (ignored: Exception) {}
        }

        // Forwarding Logic (Only on new non-duplicate transactions)
        handleSmsForwarding(appContext, prefs, packageName, messageBody, isCredit, isDebit, amountStr, senderName, utr)

        // Save latest message
        prefs.edit().putString("last_sms", "App: $packageName\n\nBody: $messageBody").apply()

        // Speak via the foreground TTS service if credit
        if (isCredit) {
            // Smart Fraud Detection — analyze before speaking
            val fraudVerdict = fraudDetector.analyze(amountStr, senderName)
            if (fraudVerdict.type != FraudType.CLEAN) {
                // Speak fraud warning instead of normal announcement
                val warningPhrase = fraudDetector.buildFraudWarningPhrase(fraudVerdict, amountStr)
                speakTts(appContext, warningPhrase)
                Log.w(TAG, "Fraud detected [${fraudVerdict.type}]: ${fraudVerdict.description}")
            } else {
                val lang = prefs.getString("language", "English") ?: "English"
                val announceSender = prefs.getBoolean("announce_sender_name", true)
                val textToRead = smsParser.getAmountFromMessageBody(appContext, messageBody, lang, announceSender, senderName)
                speakTts(appContext, textToRead)
            }
        }
    }

    private fun handleSmsForwarding(
        appContext: Context,
        prefs: android.content.SharedPreferences,
        packageName: String,
        messageBody: String,
        isCredit: Boolean,
        isDebit: Boolean,
        amount: String,
        sender: String,
        ref: String?
    ) {
        val forwardNumber = prefs.getString("forwarder_number", "") ?: ""
        if (forwardNumber.isEmpty()) return

        val appFilter = prefs.getString("forwarder_app_filter", "all") ?: "all"
        if ("all" != appFilter && packageName != appFilter) return

        val typeFilter = prefs.getString("forwarder_type_filter", "Credited") ?: "Credited"
        val shouldForward = when (typeFilter) {
            "Credited" -> isCredit
            "Debited" -> isDebit
            "Both" -> isCredit || isDebit
            else -> false
        }

        if (shouldForward) {
            try {
                val appClean = when {
                    packageName.contains("phonepe") -> "PhonePe"
                    packageName.contains("paytm") -> "Paytm"
                    packageName.contains("paisa") -> "Google Pay"
                    packageName.contains("fampay") -> "FamPay"
                    packageName.contains("bharatpe") -> "BharatPe"
                    packageName.contains("cred") -> "CRED"
                    else -> packageName.substringAfterLast('.')
                }
                val senderClean = sender.ifEmpty { "Customer" }
                val refClean = ref ?: "N/A"
                val timeStr = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val forwardMsg = "✅ UPI ALERT: ₹$amount received from $senderClean via $appClean on $timeStr. Ref: $refClean. (MyUPIAlert)"

                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                    appContext.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(forwardMsg)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(forwardNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(forwardNumber, null, forwardMsg, null, null)
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun speakTts(context: Context, text: String) {
        try {
            val svc = Intent(context, ForegroundTtsService::class.java).apply {
                action = ForegroundTtsService.ACTION_SPEAK
                putExtra(ForegroundTtsService.EXTRA_TEXT, text)
            }
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    context.startForegroundService(svc)
                } catch (e: Exception) {
                    try { context.startService(svc) } catch (ignored: Exception) {}
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        } catch (ignored: Exception) {}
    }

    companion object {
        private const val TAG = "NotificationListener"

        @JvmField
        val RECOGNIZED_UPI_PACKAGES = arrayOf(
            "com.phonepe.app", "com.phonepe.app.business",
            "net.one97.paytm", "net.one97.paytm.merchant",
            "com.google.android.apps.nbu.paisa.user", "com.google.android.apps.nbu.paisa.merchant",
            "in.org.npci.upiapp", "com.bharatpe.app",
            "com.sbi.upi", "com.sbi.upi.intent", "com.sbi.lotusintouch",
            "com.csam.icici.bank.imobile", "com.axis.mobile", "com.bob.fe",
            "com.msf.kbank.mobile", "com.lcode.ucomobilebanking",
            "com.idfcfirstbank.optimus", "com.rblbank.mobank", "com.indusind.indie",
            "in.amazon.mShop.android.shopping", "com.dreamplug.androidapp", "com.cred.android",
            "com.canarabank.mobility", "com.infrasoft.uboi", "com.cub.cuphyper", "com.fampay.in",
            "com.snapwork.hdfc", "com.hdfcbank.payzapp", "com.google.android.gm"
        )
    }
}
