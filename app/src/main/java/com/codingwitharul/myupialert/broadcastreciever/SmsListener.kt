package com.codingwitharul.myupialert.broadcastreciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import com.codingwitharul.myupialert.ForegroundTtsService
import com.codingwitharul.myupialert.dedup.DedupAction
import com.codingwitharul.myupialert.dedup.PaymentDeduplicator
import com.codingwitharul.myupialert.fraud.FraudDetector
import com.codingwitharul.myupialert.fraud.FraudType
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.repository.TransactionRepository
import com.codingwitharul.myupialert.smsparser.SmsParser
import com.codingwitharul.myupialert.widget.HeatmapWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import java.util.concurrent.Executors

class SmsListener : BroadcastReceiver(), KoinComponent {
    private val transactionRepository: TransactionRepository by inject()
    private val smsParser: SmsParser by inject()
    private val paymentDeduplicator: PaymentDeduplicator by inject()
    private val fraudDetector: FraudDetector by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messageBody = StringBuilder()
            var address = ""
            val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                address = smsMessage.originatingAddress ?: ""
                messageBody.append(smsMessage.messageBody)
            }

            val fullMessage = messageBody.toString()
            val textToDisplay = "Address: $address\n\nBody: $fullMessage"

            val isCredit = smsParser.isCreditTransaction(fullMessage)
            val isDebit = smsParser.isDebitTransaction(fullMessage)

            // Ignore SMS that are neither credit nor debit transactions
            if (!isCredit && !isDebit) {
                return
            }

            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE)

            val amountStr = smsParser.extractAmount(fullMessage)
            val senderName = smsParser.getSenderName(fullMessage).ifEmpty { address }
            val utr = smsParser.extractReferenceNumber(fullMessage)

            // Deduplication & Anti-Fraud evaluation
            val decision = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                paymentDeduplicator.evaluate(
                    amountStr = amountStr,
                    senderName = senderName,
                    utr = utr,
                    source = "sms",
                    rawBody = fullMessage
                )
            }

            when (decision.action) {
                DedupAction.SUPPRESS_TWIN -> {
                    // Silently drop twin SMS (e.g. UPI notification already announced the payment 2 seconds ago)
                    Log.d(TAG, "Suppressed twin SMS confirmation for ₹$amountStr from $address (${decision.reason})")
                    if (utr != null && decision.existingTransaction != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            paymentDeduplicator.enrichExistingTransactionIfPossible(decision.existingTransaction, utr, address)
                        }
                    }
                    return
                }
                DedupAction.WARN_FRAUD_REPLAY -> {
                    // Anti-fraud: Replay of an OLD transaction with matching UTR (> 3 minutes ago)
                    Log.w(TAG, "Duplicate replay alert: ₹$amountStr from Bank SMS with UTR $utr")
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
                sender = address,
                body = fullMessage,
                timestamp = System.currentTimeMillis()
            )

            paymentDeduplicator.recordProcessed(
                amountStr = amountStr,
                senderName = senderName,
                utr = utr,
                source = "sms",
                rawBody = fullMessage,
                transactionId = transactionId
            )

            executor.execute {
                try {
                    CoroutineScope(Dispatchers.IO).launch { transactionRepository.insert(transaction) }
                    val updateIntent = Intent("com.codingwitharul.myupialert.PAYMENT_RECEIVED").apply {
                        `package` = appContext.packageName
                    }
                    appContext.sendBroadcast(updateIntent)

                    // Refresh home screen widget with new data
                    HeatmapWidgetProvider.refreshAll(appContext)

                    // Refresh sticky status bar notification
                    ForegroundTtsService.updateNotification(appContext)
                } catch (ignored: Exception) {}
            }

            // Forwarding Logic (Only on new non-duplicate transactions)
            handleSmsForwarding(appContext, prefs, address, fullMessage, isCredit, isDebit, amountStr, senderName, utr)

            // Save latest message so UI can pick it up when opened
            prefs.edit().putString("last_sms", textToDisplay).apply()

            // Speak via the foreground TTS service if credit
            if (isCredit) {
                // Smart Fraud Detection — analyze before speaking
                val fraudVerdict = fraudDetector.analyze(amountStr, senderName)
                if (fraudVerdict.type != FraudType.CLEAN) {
                    val warningPhrase = fraudDetector.buildFraudWarningPhrase(fraudVerdict, amountStr)
                    speakTts(appContext, warningPhrase)
                    Log.w(TAG, "Fraud detected [${fraudVerdict.type}]: ${fraudVerdict.description}")
                } else {
                    val lang = prefs.getString("language", "English") ?: "English"
                    val announceSender = prefs.getBoolean("announce_sender_name", true)
                    val textToRead = smsParser.getAmountFromMessageBody(context, fullMessage, lang, announceSender, senderName)
                    speakTts(appContext, textToRead)
                }
            }
        }
    }

    private fun handleSmsForwarding(
        context: Context,
        prefs: android.content.SharedPreferences,
        address: String,
        fullMessage: String,
        isCredit: Boolean,
        isDebit: Boolean,
        amount: String,
        sender: String,
        ref: String?
    ) {
        val forwardNumber = prefs.getString("forwarder_number", "") ?: ""
        if (forwardNumber.isEmpty()) return

        val typeFilter = prefs.getString("forwarder_type_filter", "Credited") ?: "Credited"
        val shouldForward = when (typeFilter) {
            "Credited" -> isCredit
            "Debited" -> isDebit
            "Both" -> isCredit || isDebit
            else -> false
        }

        if (shouldForward) {
            try {
                val senderClean = sender.ifEmpty { address.ifEmpty { "Customer" } }
                val refClean = ref ?: "N/A"
                val timeStr = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val forwardMsg = "✅ UPI ALERT: ₹$amount received from $senderClean via Bank SMS ($address) on $timeStr. Ref: $refClean. (MyUPIAlert)"

                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                    context.getSystemService(SmsManager::class.java)
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
        private const val TAG = "SmsListener"
        private val executor = Executors.newSingleThreadExecutor()
    }
}
