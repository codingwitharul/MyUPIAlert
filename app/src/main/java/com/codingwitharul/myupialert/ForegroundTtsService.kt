// ============================================================================
// Foreground TTS Service — UPI Payment Alert
// Runs as an unkillable sticky background service to speak alerts.
// Features independent AudioManager volume override, LoudnessEnhancer 200% boost,
// and 13 Indian language support in Kotlin.
// ============================================================================
package com.codingwitharul.myupialert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.app.PendingIntent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codingwitharul.myupialert.db.TransactionDao
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.repository.SettingsRepository
import com.codingwitharul.myupialert.smsparser.AnnouncementPhrases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ForegroundTtsService : Service() {
    private val settingsRepository: SettingsRepository by inject()
    private val transactionDao: TransactionDao by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var tts: TextToSpeech? = null

    // Deduplication map: stores text -> timestamp
    private val recentAnnouncements = ConcurrentHashMap<String, Long>()

    private var originalVolume: Int? = null
    private var audioSessionId = 0
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioFocusRequest: Any? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyUPIAlert Soundbox Active")
            .setContentText("Listening for incoming payments via SMS and Notifications.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, n)
        updateStickyNotification()

        // Generate audio session ID for hardware LoudnessEnhancer
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                audioSessionId = am.generateAudioSessionId()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not generate audio session ID: ${e.message}")
        }

        var preferredEngine: String? = null
        if (TtsEngineHelper.isGoogleTtsInstalled(applicationContext)) {
            preferredEngine = TtsEngineHelper.GOOGLE_TTS_PACKAGE
        }
        tts = TextToSpeech(applicationContext, { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale("en", "IN")
                applyAudioAttributes()
                setupTtsListener()
            }
        }, preferredEngine)
    }

    private fun applyAudioAttributes() {
        if (tts != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
                tts?.setAudioAttributes(attrs)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set audio attributes: ${e.message}")
            }
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                setTtsVolume()
            }

            override fun onDone(utteranceId: String) {
                restoreVolume()
            }

            override fun onError(utteranceId: String) {
                restoreVolume()
            }
        })
    }

    @Synchronized
    private fun setTtsVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                // 1. Override silent/vibrate mode: ensure STREAM_MUSIC is unmuted
                try {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                } catch (ignored: Throwable) {}

                // 2. Request Audio Focus so background media/music ducks during announcement
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val playbackAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                        val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(false)
                            .build()
                        audioFocusRequest = focusReq
                        am.requestAudioFocus(focusReq)
                    } else {
                        @Suppress("DEPRECATION")
                        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio focus request failed: ${e.message}")
                }

                if (originalVolume == null) {
                    originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
                
                val volumePercent = settingsRepository.volume
                val effectivePercent = volumePercent.coerceIn(5, 150)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * (effectivePercent.coerceAtMost(100) / 100.0f)).toInt().coerceIn(1, maxVol)

                // Set stream volume
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                
                @Suppress("DEPRECATION")
                val isBtActive = am.isBluetoothA2dpOn || am.isBluetoothScoOn
                Log.d(TAG, "Volume set to $volumePercent% (Target stream level: $targetVol/$maxVol, BT Active: $isBtActive)")

                // Hardware LoudnessEnhancer for boost > 100%
                if (volumePercent > 100 && audioSessionId != 0) {
                    try {
                        if (loudnessEnhancer == null) {
                            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                        }
                        val boostAmount = volumePercent - 100
                        val targetGainMb = (boostAmount * 15).coerceIn(0, 1500)
                        loudnessEnhancer?.setTargetGain(targetGainMb)
                        loudnessEnhancer?.enabled = true
                        Log.d(TAG, "Hardware LoudnessEnhancer active: +${targetGainMb}mB gain (Volume: $volumePercent%)")
                    } catch (e: Exception) {
                        Log.w(TAG, "LoudnessEnhancer error: ${e.message}")
                    }
                } else {
                    try {
                        loudnessEnhancer?.enabled = false
                    } catch (ignored: Throwable) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
        }
    }

    @Synchronized
    private fun restoreVolume() {
        try {
            Thread.sleep(150)
            if (tts != null && tts?.isSpeaking == false) {
                val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null) {
                    // Abandon Audio Focus
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val req = audioFocusRequest as? AudioFocusRequest
                            if (req != null) {
                                am.abandonAudioFocusRequest(req)
                                audioFocusRequest = null
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            am.abandonAudioFocus(null)
                        }
                    } catch (ignored: Throwable) {}

                    if (originalVolume != null) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume!!, 0)
                        Log.d(TAG, "Volume restored to original level: $originalVolume")
                        originalVolume = null
                    }
                }
                try {
                    loudnessEnhancer?.enabled = false
                } catch (ignored: Throwable) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring volume: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "UPI Payment Alert Service", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Keeps the app active to listen for payments and announce them."
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_SPEAK -> {
                    val text = intent.getStringExtra(EXTRA_TEXT)
                    val isForceSpeak = intent.getBooleanExtra(EXTRA_FORCE_SPEAK, false)
                    if (!text.isNullOrEmpty()) {
                        handleSpeakRequest(text, isForceSpeak)
                    }
                }
                ACTION_UPDATE_NOTIFICATION -> {
                    updateStickyNotification()
                }
                ACTION_REPEAT_LAST -> {
                    handleRepeatLast()
                }
                ACTION_TOGGLE_MUTE -> {
                    handleToggleMute()
                }
            }
        }
        return START_STICKY
    }

    private fun handleSpeakRequest(text: String, isForceSpeak: Boolean = false) {
        
        if (!isForceSpeak && (!settingsRepository.isServiceActiveNow() || settingsRepository.isMuted)) {
            Log.d(TAG, "Suppressed - service inactive, outside scheduled hours, or muted: $text")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Clean up old entries
        for ((key, value) in recentAnnouncements) {
            if (currentTime - value > DEDUPLICATION_WINDOW_MS) {
                recentAnnouncements.remove(key)
            }
        }

        // Deduplication check: bypass when force repeating
        val pipeMatch = Regex("""\|([^|]+)\|""").find(text)?.groupValues?.getOrNull(1)
        val amountKey = if (pipeMatch != null) "AMT_$pipeMatch" else null

        if (!isForceSpeak && (recentAnnouncements.containsKey(text) || (amountKey != null && recentAnnouncements[amountKey]?.let { currentTime - it < 6000L } == true))) {
            Log.d(TAG, "Duplicate announcement prevented: $text")
            return
        }

        // Add to recent announcements
        recentAnnouncements[text] = currentTime
        if (amountKey != null) {
            recentAnnouncements[amountKey] = currentTime
        }

        // 1. Play crystal chime sound effect before speech if enabled
        if (settingsRepository.playChimeBeforeVoice) {
            AudioChimeHelper.playPaymentChime(settingsRepository.volume)
        }

        // 2. Flashlight strobe notification if enabled
        if (settingsRepository.strobeFlashlightOnPayment) {
            AudioChimeHelper.strobeFlashlight(applicationContext, times = 3)
        }

        // 3. Prepend custom store name if configured
        val store = settingsRepository.storeName.trim()
        val speechText = if (store.isNotEmpty() && !text.contains(store, ignoreCase = true) && !text.startsWith("Warning", ignoreCase = true)) {
            "$store: $text"
        } else {
            text
        }

        // Announce
        speak(speechText)
        updateStickyNotification()
    }

    private fun speak(text: String) {
        try {
            val localTts = tts ?: return
            

            // Handle 13 Indian Languages
            val lang = settingsRepository.language
            val lower = lang.lowercase(Locale.ROOT)
            val locale = when {
                lower.contains("hindi") -> Locale("hi", "IN")
                lower.contains("tamil") -> Locale("ta", "IN")
                lower.contains("telugu") -> Locale("te", "IN")
                lower.contains("kannada") -> Locale("kn", "IN")
                lower.contains("malayalam") -> Locale("ml", "IN")
                lower.contains("gujarati") -> Locale("gu", "IN")
                lower.contains("punjabi") -> Locale("pa", "IN")
                lower.contains("bengali") -> Locale("bn", "IN")
                lower.contains("marathi") -> Locale("mr", "IN")
                lower.contains("odia") -> Locale("or", "IN")
                lower.contains("assamese") -> Locale("as", "IN")
                lower.contains("urdu") -> Locale("ur", "IN")
                else -> Locale("en", "IN")
            }

            var res = localTts.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                res = localTts.setLanguage(Locale(locale.language))
            }

            // Bind matching native voice
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val voices = localTts.voices
                    if (voices != null) {
                        for (voice in voices) {
                            if (voice?.locale != null && voice.locale.language.equals(locale.language, ignoreCase = true)) {
                                localTts.voice = voice
                                res = TextToSpeech.LANG_AVAILABLE
                                Log.d(TAG, "Bound native TTS voice: ${voice.name} for ${locale.displayLanguage}")
                                break
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }

            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                localTts.setLanguage(Locale("en", "IN"))
            }

            // Handle Volume & Session ID
            val volumePercent = settingsRepository.volume
            val volumeFloat = (volumePercent / 100f).coerceIn(0.05f, 1.0f)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeFloat)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                if (audioSessionId != 0) {
                    putInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, audioSessionId)
                }
            }

            val customSpeed = settingsRepository.speedFloat
            localTts.setPitch(1.0f)

            if (text.contains("|")) {
                val segments = text.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in segments.indices) {
                    val segment = segments[i]
                    if (segment.isEmpty()) continue

                    // Odd indices are amounts read at customSpeed - 0.1x
                    // Even indices are structural words read at baseline speed (0.9x)
                    val currentSpeed = if (i % 2 == 1) (customSpeed - 0.1f).coerceAtLeast(0.1f) else 0.9f

                    localTts.setSpeechRate(currentSpeed)
                    localTts.speak(segment, TextToSpeech.QUEUE_ADD, params, "FG_UPI_TTS_${System.currentTimeMillis()}_$i")
                }
            } else {
                localTts.setSpeechRate(customSpeed)
                localTts.speak(text, TextToSpeech.QUEUE_ADD, params, "FG_UPI_TTS_${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS Error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (ignored: Throwable) {}
        tts?.shutdown()
        tts = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleRepeatLast() {
        serviceScope.launch {
            val latest = transactionDao.getLatestTransactionSync()
            if (latest != null && latest.type.equals("credit", ignoreCase = true)) {
                val amountStr = latest.amount ?: "0"
                val parts = amountStr.split("\\.".toRegex(), limit = 2)
                val rupees = parts[0]
                val paisa = if (parts.size > 1 && parts[1] != "0" && parts[1] != "00") parts[1] else null
                val lang = settingsRepository.language
                val senderName = extractCleanSender(latest.sender)
                val announceSender = settingsRepository.announceSenderName && senderName.isNotBlank()

                val phrase = AnnouncementPhrases.buildPhrase(
                    languageStr = lang,
                    amount = "|$rupees|",
                    paisa = paisa?.let { "|$it|" },
                    senderName = if (announceSender) senderName else null
                )
                handleSpeakRequest(phrase, isForceSpeak = true)
            } else {
                handleSpeakRequest("No recent payment found to repeat", isForceSpeak = true)
            }
        }
    }

    private fun handleToggleMute() {
        val newMuted = !settingsRepository.isMuted
        settingsRepository.isMuted = newMuted
        val feedback = if (newMuted) "Soundbox announcements muted" else "Soundbox announcements unmuted"
        handleSpeakRequest(feedback, isForceSpeak = true)
        updateStickyNotification()
    }

    fun updateStickyNotification() {
        serviceScope.launch {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@launch
                val notification = buildServiceNotification()
                nm.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update sticky notification: ${e.message}")
            }
        }
    }

    private suspend fun buildServiceNotification(): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settingsRepository.isStickyStatusBarEnabled) {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MyUPIAlert Soundbox Active")
                .setContentText("Listening for incoming UPI payments.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(openAppPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        // Calculate Today's Stats
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val todayTxns = try {
            transactionDao.getTransactionsByDateSync(startOfDay, endOfDay)
        } catch (e: Exception) {
            emptyList()
        }
        val latestTxn = try {
            transactionDao.getLatestTransactionSync()
        } catch (e: Exception) {
            null
        }

        var todayTotal = 0.0
        var todayCredits = 0
        for (t in todayTxns) {
            if (t.type.equals("credit", ignoreCase = true)) {
                todayCredits++
                val amt = t.amount?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                todayTotal += amt
            }
        }

        val totalStr = formatIndianCurrency(todayTotal)
        val isLive = settingsRepository.isServiceActiveNow()
        val isMuted = settingsRepository.isMuted
        val storeName = settingsRepository.storeName.trim()

        val statusTag = when {
            !isLive -> "⏸️ PAUSED"
            isMuted -> "🔇 MUTED"
            else -> "🟢 LIVE"
        }

        val title = if (storeName.isNotBlank()) {
            "$storeName • $statusTag"
        } else {
            "MyUPIAlert Soundbox • $statusTag"
        }

        val lastSender = extractCleanSender(latestTxn?.sender)
        val lastAmt = latestTxn?.amount ?: "0"

        val shortInfo = if (latestTxn != null) {
            "Today: ₹$totalStr ($todayCredits txns) • Last: +₹$lastAmt ($lastSender)"
        } else {
            "Today: ₹$totalStr ($todayCredits txns) • Ready for payments"
        }

        val bigText = StringBuilder()
        bigText.append("Today's Total: ₹$totalStr from $todayCredits payments\n")
        if (latestTxn != null) {
            bigText.append("Latest Payment: +₹$lastAmt via $lastSender\n")
        }
        bigText.append("Voice: ${settingsRepository.language} | Soundbox: ${if (isMuted) "Muted" else if (isLive) "Active" else "Scheduled Off"}")

        // Repeat Last Action
        val repeatIntent = Intent(this, ForegroundTtsService::class.java).apply {
            action = ACTION_REPEAT_LAST
        }
        val repeatPending = PendingIntent.getService(
            this, 1, repeatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute / Unmute Action
        val muteIntent = Intent(this, ForegroundTtsService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePending = PendingIntent.getService(
            this, 2, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(shortInfo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_revert, "Repeat Last", repeatPending)
            .addAction(
                if (isMuted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode,
                if (isMuted) "Unmute" else "Mute",
                mutePending
            )
            .build()
    }

    private fun extractCleanSender(sender: String?): String {
        if (sender.isNullOrBlank()) return "UPI"
        val lower = sender.lowercase(Locale.ROOT)
        return when {
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("paisa") || lower.contains("gpay") || lower.contains("google pay") -> "Google Pay"
            lower.contains("bhim") -> "BHIM"
            lower.contains("cred") -> "CRED"
            lower.contains("bharatpe") -> "BharatPe"
            lower.contains("fampay") -> "FamPay"
            lower.contains("hdfc") -> "HDFC"
            lower.contains("sbi") -> "SBI"
            lower.contains("icici") -> "ICICI"
            else -> sender.substringAfterLast('.').take(12)
        }
    }

    private fun formatIndianCurrency(amount: Double): String {
        val whole = amount.toLong()
        val numStr = whole.toString()
        if (numStr.length <= 3) return numStr
        val lastThree = numStr.takeLast(3)
        val rest = numStr.dropLast(3)
        val formattedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$formattedRest,$lastThree"
    }

    companion object {
        const val NOTIFICATION_ID = 12345
        const val ACTION_SPEAK = "com.codingwitharul.myupialert.action.SPEAK"
        const val ACTION_UPDATE_NOTIFICATION = "com.codingwitharul.myupialert.action.UPDATE_NOTIFICATION"
        const val ACTION_REPEAT_LAST = "com.codingwitharul.myupialert.action.REPEAT_LAST"
        const val ACTION_TOGGLE_MUTE = "com.codingwitharul.myupialert.action.TOGGLE_MUTE"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_FORCE_SPEAK = "extra_force_speak"
        private const val CHANNEL_ID = "upi_tts_channel"
        private const val TAG = "UPIPaymentAlert"
        private const val DEDUPLICATION_WINDOW_MS = 60000L // 60 seconds

        fun updateNotification(context: Context) {
            val intent = Intent(context, ForegroundTtsService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (ignored: Exception) {}
        }
    }
}
