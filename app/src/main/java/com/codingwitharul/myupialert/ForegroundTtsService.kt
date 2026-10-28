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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

import com.codingwitharul.myupialert.repository.SettingsRepository
import org.koin.android.ext.android.inject

class ForegroundTtsService : Service() {
    private val settingsRepository: SettingsRepository by inject()

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
            .setContentTitle("UPI Payment Alert Active")
            .setContentText("Listening for incoming payments via SMS and Notifications.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
        startForeground(12345, n)

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
        if (intent != null && ACTION_SPEAK == intent.action) {
            val text = intent.getStringExtra(EXTRA_TEXT)
            val isForceSpeak = intent.getBooleanExtra(EXTRA_FORCE_SPEAK, false)
            if (!text.isNullOrEmpty()) {
                handleSpeakRequest(text, isForceSpeak)
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

    companion object {
        const val ACTION_SPEAK = "com.codingwitharul.myupialert.action.SPEAK"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_FORCE_SPEAK = "extra_force_speak"
        private const val CHANNEL_ID = "upi_tts_channel"
        private const val TAG = "UPIPaymentAlert"
        private const val DEDUPLICATION_WINDOW_MS = 60000L // 60 seconds
    }
}
