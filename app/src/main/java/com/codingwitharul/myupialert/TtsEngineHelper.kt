package com.codingwitharul.myupialert

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * TtsEngineHelper manages Google TTS detection, silent pre-warming of Indian language voice models,
 * and engine switching in Kotlin.
 */
object TtsEngineHelper {

    const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    const val SAMSUNG_TTS_PACKAGE = "com.samsung.SMT"
    private const val TAG = "TtsEngineHelper"

    /** Check if Google Speech Services is installed */
    @JvmStatic
    fun isGoogleTtsInstalled(context: Context?): Boolean {
        return isPackageInstalled(context, GOOGLE_TTS_PACKAGE)
    }

    @JvmStatic
    fun isPackageInstalled(context: Context?, packageName: String?): Boolean {
        if (context == null || packageName == null) return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pre-warms the Google TTS engine and triggers background voice data checks for Indian languages.
     */
    @JvmStatic
    fun prewarmAndPrepareVoiceData(context: Context?) {
        if (context == null) return
        val appContext = context.applicationContext

        Thread {
            try {
                if (isGoogleTtsInstalled(appContext)) {
                    Log.d(TAG, "Google TTS installed. Configuring as preferred engine and warming up...")
                    val prefs = appContext.getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE)
                    prefs.edit().putString("tts_engine", GOOGLE_TTS_PACKAGE).apply()

                    val warmupTts = arrayOfNulls<TextToSpeech>(1)
                    warmupTts[0] = TextToSpeech(appContext, { status ->
                        if (status == TextToSpeech.SUCCESS && warmupTts[0] != null) {
                            Log.d(TAG, "Google TTS warmed up successfully. Checking Indian language voices...")
                            val indianLocales = arrayOf(
                                Locale("hi", "IN"), Locale("gu", "IN"), Locale("mr", "IN"),
                                Locale("ta", "IN"), Locale("te", "IN"), Locale("kn", "IN"),
                                Locale("ml", "IN"), Locale("pa", "IN"), Locale("bn", "IN"),
                                Locale("or", "IN"), Locale("as", "IN"), Locale("ur", "IN")
                            )
                            for (loc in indianLocales) {
                                try {
                                    warmupTts[0]?.isLanguageAvailable(loc)
                                } catch (ignored: Exception) {}
                            }

                            // Trigger Google TTS background voice download check if needed
                            try {
                                val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
                                checkIntent.setPackage(GOOGLE_TTS_PACKAGE)
                                checkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                appContext.startActivity(checkIntent)
                            } catch (ignored: Exception) {}

                            // Close warmup instance after 3 seconds
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    warmupTts[0]?.shutdown()
                                    warmupTts[0] = null
                                } catch (ignored: Exception) {}
                            }, 3000)
                        }
                    }, GOOGLE_TTS_PACKAGE)
                } else {
                    Log.w(TAG, "Google TTS not installed on device! Flagging for install...")
                    val prefs = appContext.getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("needs_google_tts_install", true).apply()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during TTS prewarm: ${e.message}")
            }
        }.start()
    }

    /**
     * Opens Play Store for Google Speech Services
     */
    @JvmStatic
    fun openGoogleTtsInPlayStore(context: Context?) {
        if (context == null) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS_PACKAGE"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS_PACKAGE")
                )
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            } catch (ignored: Exception) {}
        }
    }
}
