package com.codingwitharul.myupialert.repository

import android.content.SharedPreferences

class SettingsRepository(private val prefs: SharedPreferences) {
    var isMuted: Boolean
        get() = prefs.getBoolean("is_muted", false)
        set(value) = prefs.edit().putBoolean("is_muted", value).apply()

    var announceSenderName: Boolean
        get() = prefs.getBoolean("announce_sender_name", true)
        set(value) = prefs.edit().putBoolean("announce_sender_name", value).apply()

    var language: String
        get() = prefs.getString("language", "English") ?: "English"
        set(value) = prefs.edit().putString("language", value).apply()

    var volume: Int
        get() = prefs.getInt("volume", 50)
        set(value) = prefs.edit().putInt("volume", value).apply()

    var speedInt: Int
        get() = prefs.getInt("speed_int", 50)
        set(value) = prefs.edit().putInt("speed_int", value).apply()

    var speedFloat: Float
        get() = prefs.getFloat("speed_float", 1.0f)
        set(value) = prefs.edit().putFloat("speed_float", value).apply()

    var forwarderNumber: String
        get() = prefs.getString("forwarder_number", "") ?: ""
        set(value) = prefs.edit().putString("forwarder_number", value).apply()

    var forwarderAppFilter: String
        get() = prefs.getString("forwarder_app_filter", "all") ?: "all"
        set(value) = prefs.edit().putString("forwarder_app_filter", value).apply()

    var forwarderTypeFilter: String
        get() = prefs.getString("forwarder_type_filter", "Both") ?: "Both"
        set(value) = prefs.edit().putString("forwarder_type_filter", value).apply()

    var hasPromptedAutostart: Boolean
        get() = prefs.getBoolean("has_prompted_autostart", false)
        set(value) = prefs.edit().putBoolean("has_prompted_autostart", value).apply()

    var playChimeBeforeVoice: Boolean
        get() = prefs.getBoolean("play_chime_before_voice", true)
        set(value) = prefs.edit().putBoolean("play_chime_before_voice", value).apply()

    var storeName: String
        get() = prefs.getString("store_name", "") ?: ""
        set(value) = prefs.edit().putString("store_name", value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("is_onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("is_onboarding_completed", value).apply()

    var strobeFlashlightOnPayment: Boolean
        get() = prefs.getBoolean("strobe_flashlight_on_payment", false)
        set(value) = prefs.edit().putBoolean("strobe_flashlight_on_payment", value).apply()

    var detectDuplicateTransactions: Boolean
        get() = prefs.getBoolean("detect_duplicate_transactions", true)
        set(value) = prefs.edit().putBoolean("detect_duplicate_transactions", value).apply()

    var soundboxKeepScreenOn: Boolean
        get() = prefs.getBoolean("soundbox_keep_screen_on", true)
        set(value) = prefs.edit().putBoolean("soundbox_keep_screen_on", value).apply()

    var bluetoothKeepAlive: Boolean
        get() = prefs.getBoolean("bluetooth_keep_alive", false)
        set(value) = prefs.edit().putBoolean("bluetooth_keep_alive", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", true)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("is_service_enabled", true)
        set(value) = prefs.edit().putBoolean("is_service_enabled", value).apply()

    var isScheduleEnabled: Boolean
        get() = prefs.getBoolean("is_schedule_enabled", false)
        set(value) = prefs.edit().putBoolean("is_schedule_enabled", value).apply()

    var scheduleStartHour: Int
        get() = prefs.getInt("schedule_start_hour", 9)
        set(value) = prefs.edit().putInt("schedule_start_hour", value).apply()

    var scheduleStartMinute: Int
        get() = prefs.getInt("schedule_start_minute", 0)
        set(value) = prefs.edit().putInt("schedule_start_minute", value).apply()

    var scheduleEndHour: Int
        get() = prefs.getInt("schedule_end_hour", 22)
        set(value) = prefs.edit().putInt("schedule_end_hour", value).apply()

    var scheduleEndMinute: Int
        get() = prefs.getInt("schedule_end_minute", 0)
        set(value) = prefs.edit().putInt("schedule_end_minute", value).apply()

    var isStickyStatusBarEnabled: Boolean
        get() = prefs.getBoolean("is_sticky_status_bar_enabled", true)
        set(value) = prefs.edit().putBoolean("is_sticky_status_bar_enabled", value).apply()

    fun isServiceActiveNow(): Boolean {
        if (!isServiceEnabled) return false
        if (!isScheduleEnabled) return true
        val cal = java.util.Calendar.getInstance()
        val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val startMinutes = scheduleStartHour * 60 + scheduleStartMinute
        val endMinutes = scheduleEndHour * 60 + scheduleEndMinute
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight active schedule, e.g., 8 PM to 4 AM
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
