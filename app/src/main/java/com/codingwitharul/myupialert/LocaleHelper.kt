package com.codingwitharul.myupialert

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * LocaleHelper manages dynamic in-app language switching across all 13 Indian languages in Kotlin.
 */
object LocaleHelper {

    const val PREFS_NAME = "UPI_PREFS"
    const val KEY_LANGUAGE = "app_ui_language"
    const val DEFAULT_LANGUAGE = "en"

    @JvmField
    val SUPPORTED_CODES = arrayOf(
        "en", "hi", "gu", "mr", "ta", "te", "kn", "ml", "pa", "bn", "or", "as", "ur"
    )

    @JvmField
    val SUPPORTED_NAMES = arrayOf(
        "English",
        "हिन्दी (Hindi)",
        "ગુજરાતી (Gujarati)",
        "मराठी (Marathi)",
        "தமிழ் (Tamil)",
        "తెలుగు (Telugu)",
        "ಕನ್ನಡ (Kannada)",
        "മലയാളം (Malayalam)",
        "ਪੰਜਾਬੀ (Punjabi)",
        "বাংলা (Bengali)",
        "ଓଡ଼ିଆ (Odia)",
        "অসমীয়া (Assamese)",
        "اردو (Urdu)"
    )

    /**
     * Reads the chosen language code from preferences.
     */
    @JvmStatic
    fun getLanguage(context: Context?): String {
        if (context == null) return DEFAULT_LANGUAGE
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Persists chosen language, updates system configuration, and restarts activity.
     */
    @JvmStatic
    fun setAppLanguage(context: Context?, langCode: String?, recreateActivity: Boolean) {
        if (context == null || langCode == null) return

        // 1. Persist to preferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, langCode)
            .commit()

        // 2. Set default JVM locale
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        // 3. AndroidX AppCompat application locales
        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        } catch (ignored: Throwable) {}

        // 4. Update Application & Activity resources directly
        updateResources(context, locale)

        // 5. Recreate / restart activity if requested
        if (recreateActivity && context is Activity) {
            val intent = Intent(context, context.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            context.finish()
            context.startActivity(intent)
        }
    }

    /**
     * Wraps context with localized configuration (for attachBaseContext).
     */
    @JvmStatic
    fun applyLocale(baseContext: Context?): Context? {
        if (baseContext == null) return null
        val langCode = getLanguage(baseContext)
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        val localizedContext = baseContext.createConfigurationContext(config)
        updateResources(baseContext, locale)
        return localizedContext
    }

    /**
     * Updates resources configuration to ensure immediate string resolution.
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun updateResources(context: Context, locale: Locale): Configuration {
        val res = context.resources
        val config = Configuration(res.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.locale = locale
        }
        config.setLayoutDirection(locale)
        res.updateConfiguration(config, res.displayMetrics)

        val appCtx = context.applicationContext
        if (appCtx != null && appCtx !== context) {
            val appRes = appCtx.resources
            val appConfig = Configuration(appRes.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appConfig.setLocales(LocaleList(locale))
            } else {
                appConfig.locale = locale
            }
            appConfig.setLayoutDirection(locale)
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        }
        return config
    }
}
