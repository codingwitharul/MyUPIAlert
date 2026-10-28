package com.codingwitharul.myupialert.ui

import com.codingwitharul.myupialert.model.PaymentTransaction

fun getTodayStartTimestamp(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun getTodayEndTimestamp(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
    cal.set(java.util.Calendar.MINUTE, 59)
    cal.set(java.util.Calendar.SECOND, 59)
    cal.set(java.util.Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

data class MainState(
    val transactions: List<PaymentTransaction> = emptyList(),
    val appFilter: String = "all",
    val dateFilterStart: Long = getTodayStartTimestamp(),
    val dateFilterEnd: Long = getTodayEndTimestamp(),
    val dateFilterLabel: String = "Today",
    
    // Settings
    val isMuted: Boolean = false,
    val announceSenderName: Boolean = true,
    val language: String = "English",
    val volume: Int = 50,
    val speedInt: Int = 50,
    val speedFloat: Float = 1.0f,
    val forwarderNumber: String = "",
    val forwarderAppFilter: String = "all",
    val forwarderTypeFilter: String = "Both",
    
    val hasPromptedAutostart: Boolean = false,
    
    // Merchant & Audio Enhancements
    val storeName: String = "",
    val playChimeBeforeVoice: Boolean = true,
    val strobeFlashlightOnPayment: Boolean = false,
    val detectDuplicateTransactions: Boolean = true,
    val soundboxKeepScreenOn: Boolean = true,
    val bluetoothKeepAlive: Boolean = false,
    val isDarkMode: Boolean = true,
    
    // Service Override & Schedule Active Hours
    val isServiceEnabled: Boolean = true,
    val isScheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 22,
    val scheduleEndMinute: Int = 0,

    // Permissions
    val allPermissionsGranted: Boolean = false,
    val smsPermissionGranted: Boolean = false,
    val notificationListenerGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false
) {
    val isServiceActiveNow: Boolean
        get() {
            if (!isServiceEnabled) return false
            if (!isScheduleEnabled) return true
            val cal = java.util.Calendar.getInstance()
            val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val startMinutes = scheduleStartHour * 60 + scheduleStartMinute
            val endMinutes = scheduleEndHour * 60 + scheduleEndMinute
            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        }
}

sealed interface MainIntent {
    data class ChangeAppFilter(val appName: String) : MainIntent
    data class ChangeDateFilter(val start: Long, val end: Long, val label: String) : MainIntent
    data class ToggleTheme(val isDark: Boolean) : MainIntent
    
    // Service Override & Schedule Intents
    data class ToggleServiceEnabled(val enabled: Boolean) : MainIntent
    data class ToggleScheduleEnabled(val enabled: Boolean) : MainIntent
    data class UpdateScheduleTime(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int) : MainIntent
    
    // Settings updates
    data class ToggleMute(val isMuted: Boolean) : MainIntent
    data class ToggleAnnounceSender(val announce: Boolean) : MainIntent
    data class ChangeLanguage(val language: String) : MainIntent
    data class ChangeVolume(val volume: Int) : MainIntent
    data class ChangeSpeed(val speedInt: Int, val speedFloat: Float) : MainIntent
    data class ChangeStoreName(val name: String) : MainIntent
    data class ToggleChime(val enabled: Boolean) : MainIntent
    data class ToggleStrobeFlashlight(val enabled: Boolean) : MainIntent
    data class ToggleDetectDuplicates(val enabled: Boolean) : MainIntent
    data class ToggleSoundboxKeepScreenOn(val enabled: Boolean) : MainIntent
    data class ToggleBluetoothKeepAlive(val enabled: Boolean) : MainIntent
    
    // Forwarder updates
    data class ChangeForwarderNumber(val number: String) : MainIntent
    data class ChangeForwarderApp(val appFilter: String) : MainIntent
    data class ChangeForwarderType(val typeFilter: String) : MainIntent
    
    // Permissions & actions
    data class SetPermissionsGranted(val granted: Boolean) : MainIntent
    data class UpdatePermissionStatus(
        val smsGranted: Boolean,
        val notificationListenerGranted: Boolean,
        val batteryIgnored: Boolean
    ) : MainIntent
    data class SetHasPromptedAutostart(val prompted: Boolean) : MainIntent
    
    object ExportTransactions : MainIntent
    object ExportCsv : MainIntent
    object SpeakTodaySummary : MainIntent
    object RepeatLastPayment : MainIntent
    data class TestSound(val amount: String?) : MainIntent
}

sealed interface MainEffect {
    data class ShowToast(val message: String, val isLong: Boolean = false) : MainEffect
    object RequestPermissions : MainEffect
    object OpenNotificationSettings : MainEffect
    object OpenAutoStartSettings : MainEffect
    object RequestBatteryOptimization : MainEffect
    data class Speak(val phrase: String, val isForce: Boolean = false) : MainEffect
    data class ExportHtml(val transactions: List<PaymentTransaction>) : MainEffect
    data class ExportCsvFile(val transactions: List<PaymentTransaction>) : MainEffect
}
