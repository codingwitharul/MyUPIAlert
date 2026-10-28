package com.codingwitharul.myupialert.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwitharul.myupialert.repository.SettingsRepository
import com.codingwitharul.myupialert.repository.TransactionRepository
import com.codingwitharul.myupialert.smsparser.AnnouncementPhrases
import com.codingwitharul.myupialert.smsparser.SmsParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class MainViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val smsParser: SmsParser = SmsParser()
) : ViewModel() {

    private val _state = MutableStateFlow(MainState(
        isMuted = settingsRepository.isMuted,
        announceSenderName = settingsRepository.announceSenderName,
        language = settingsRepository.language,
        volume = settingsRepository.volume,
        speedInt = settingsRepository.speedInt,
        speedFloat = settingsRepository.speedFloat,
        forwarderNumber = settingsRepository.forwarderNumber,
        forwarderAppFilter = settingsRepository.forwarderAppFilter,
        forwarderTypeFilter = settingsRepository.forwarderTypeFilter,
        hasPromptedAutostart = settingsRepository.hasPromptedAutostart,
        storeName = settingsRepository.storeName,
        playChimeBeforeVoice = settingsRepository.playChimeBeforeVoice,
        strobeFlashlightOnPayment = settingsRepository.strobeFlashlightOnPayment,
        detectDuplicateTransactions = settingsRepository.detectDuplicateTransactions,
        soundboxKeepScreenOn = settingsRepository.soundboxKeepScreenOn,
        bluetoothKeepAlive = settingsRepository.bluetoothKeepAlive,
        isDarkMode = settingsRepository.isDarkMode,
        isServiceEnabled = settingsRepository.isServiceEnabled,
        isScheduleEnabled = settingsRepository.isScheduleEnabled,
        scheduleStartHour = settingsRepository.scheduleStartHour,
        scheduleStartMinute = settingsRepository.scheduleStartMinute,
        scheduleEndHour = settingsRepository.scheduleEndHour,
        scheduleEndMinute = settingsRepository.scheduleEndMinute
    ))
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MainEffect>()
    val effect: SharedFlow<MainEffect> = _effect.asSharedFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            val currentState = _state.value
            transactionRepository.getTransactions(
                currentState.appFilter,
                currentState.dateFilterStart,
                currentState.dateFilterEnd
            ).collectLatest { transactions ->
                _state.update { it.copy(transactions = transactions) }
            }
        }
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ToggleServiceEnabled -> {
                settingsRepository.isServiceEnabled = intent.enabled
                _state.update { it.copy(isServiceEnabled = intent.enabled) }
            }
            is MainIntent.ToggleScheduleEnabled -> {
                settingsRepository.isScheduleEnabled = intent.enabled
                _state.update { it.copy(isScheduleEnabled = intent.enabled) }
            }
            is MainIntent.UpdateScheduleTime -> {
                settingsRepository.scheduleStartHour = intent.startHour
                settingsRepository.scheduleStartMinute = intent.startMinute
                settingsRepository.scheduleEndHour = intent.endHour
                settingsRepository.scheduleEndMinute = intent.endMinute
                _state.update {
                    it.copy(
                        scheduleStartHour = intent.startHour,
                        scheduleStartMinute = intent.startMinute,
                        scheduleEndHour = intent.endHour,
                        scheduleEndMinute = intent.endMinute
                    )
                }
            }
            is MainIntent.ToggleTheme -> {
                settingsRepository.isDarkMode = intent.isDark
                _state.update { it.copy(isDarkMode = intent.isDark) }
            }
            is MainIntent.ChangeAppFilter -> {
                _state.update { it.copy(appFilter = intent.appName) }
                loadTransactions()
            }
            is MainIntent.ChangeDateFilter -> {
                _state.update { it.copy(dateFilterStart = intent.start, dateFilterEnd = intent.end, dateFilterLabel = intent.label) }
                loadTransactions()
            }
            is MainIntent.ChangeLanguage -> {
                settingsRepository.language = intent.language
                _state.update { it.copy(language = intent.language) }
            }
            is MainIntent.ChangeSpeed -> {
                settingsRepository.speedInt = intent.speedInt
                settingsRepository.speedFloat = intent.speedFloat
                _state.update { it.copy(speedInt = intent.speedInt, speedFloat = intent.speedFloat) }
            }
            is MainIntent.ChangeVolume -> {
                settingsRepository.volume = intent.volume
                _state.update { it.copy(volume = intent.volume) }
            }
            is MainIntent.ToggleAnnounceSender -> {
                settingsRepository.announceSenderName = intent.announce
                _state.update { it.copy(announceSenderName = intent.announce) }
            }
            is MainIntent.ToggleMute -> {
                settingsRepository.isMuted = intent.isMuted
                _state.update { it.copy(isMuted = intent.isMuted) }
            }
            is MainIntent.ChangeForwarderApp -> {
                settingsRepository.forwarderAppFilter = intent.appFilter
                _state.update { it.copy(forwarderAppFilter = intent.appFilter) }
            }
            is MainIntent.ChangeForwarderNumber -> {
                settingsRepository.forwarderNumber = intent.number
                _state.update { it.copy(forwarderNumber = intent.number) }
            }
            is MainIntent.ChangeForwarderType -> {
                settingsRepository.forwarderTypeFilter = intent.typeFilter
                _state.update { it.copy(forwarderTypeFilter = intent.typeFilter) }
            }
            is MainIntent.SetHasPromptedAutostart -> {
                settingsRepository.hasPromptedAutostart = intent.prompted
                _state.update { it.copy(hasPromptedAutostart = intent.prompted) }
            }
            is MainIntent.SetPermissionsGranted -> {
                _state.update { it.copy(allPermissionsGranted = intent.granted) }
            }
            is MainIntent.UpdatePermissionStatus -> {
                val allGranted = intent.smsGranted && intent.notificationListenerGranted
                _state.update {
                    it.copy(
                        allPermissionsGranted = allGranted,
                        smsPermissionGranted = intent.smsGranted,
                        notificationListenerGranted = intent.notificationListenerGranted,
                        batteryOptimizationIgnored = intent.batteryIgnored
                    )
                }
            }
            is MainIntent.ChangeStoreName -> {
                settingsRepository.storeName = intent.name
                _state.update { it.copy(storeName = intent.name) }
            }
            is MainIntent.ToggleChime -> {
                settingsRepository.playChimeBeforeVoice = intent.enabled
                _state.update { it.copy(playChimeBeforeVoice = intent.enabled) }
            }
            is MainIntent.ToggleStrobeFlashlight -> {
                settingsRepository.strobeFlashlightOnPayment = intent.enabled
                _state.update { it.copy(strobeFlashlightOnPayment = intent.enabled) }
            }
            is MainIntent.ToggleDetectDuplicates -> {
                settingsRepository.detectDuplicateTransactions = intent.enabled
                _state.update { it.copy(detectDuplicateTransactions = intent.enabled) }
            }
            is MainIntent.ToggleSoundboxKeepScreenOn -> {
                settingsRepository.soundboxKeepScreenOn = intent.enabled
                _state.update { it.copy(soundboxKeepScreenOn = intent.enabled) }
            }
            is MainIntent.ToggleBluetoothKeepAlive -> {
                settingsRepository.bluetoothKeepAlive = intent.enabled
                _state.update { it.copy(bluetoothKeepAlive = intent.enabled) }
            }
            MainIntent.ExportTransactions -> {
                sendEffect(MainEffect.ExportHtml(_state.value.transactions))
            }
            MainIntent.ExportCsv -> {
                sendEffect(MainEffect.ExportCsvFile(_state.value.transactions))
            }
            MainIntent.SpeakTodaySummary -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                val todayTxns = _state.value.transactions.filter { it.timestamp >= startOfDay && it.type == "credit" }
                val totalAmt = todayTxns.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
                val count = todayTxns.size

                val phrase = if (count > 0) {
                    "Today's total collection is |${totalAmt.toInt()}| rupees from $count payments."
                } else {
                    "No payments received today yet."
                }
                sendEffect(MainEffect.Speak(phrase))
            }
            is MainIntent.TestSound -> {
                // Determine phrase and send Speak effect
                val amt = if (intent.amount.isNullOrBlank()) "150" else intent.amount.trim()
                val fakeBody = if (intent.amount == null) "Sample test announcement from UPI Payment Alert" else "Rs $amt credited to your A/c"
                sendEffect(MainEffect.Speak(fakeBody))
            }
            MainIntent.RepeatLastPayment -> {
                val lastCredit = _state.value.transactions.firstOrNull { it.type == "credit" }
                if (lastCredit != null) {
                    val body = lastCredit.body
                    val amountStr = lastCredit.amount ?: smsParser.extractAmount(body)
                    val rawSender = smsParser.getSenderName(body)
                    val senderName = if (rawSender.isNotBlank()) rawSender else null
                    
                    val parts = amountStr.split("\\.".toRegex(), limit = 2)
                    val rupees = parts[0]
                    val paisa = if (parts.size > 1 && parts[1] != "0" && parts[1] != "00") parts[1] else null
                    val lang = settingsRepository.language
                    val announceSender = settingsRepository.announceSenderName && !senderName.isNullOrBlank()
                    
                    val phrase = AnnouncementPhrases.buildPhrase(
                        languageStr = lang,
                        amount = "|$rupees|",
                        paisa = paisa?.let { "|$it|" },
                        senderName = if (announceSender) senderName else null
                    )
                    sendEffect(MainEffect.Speak(phrase, isForce = true))
                    sendEffect(MainEffect.ShowToast("Repeating last payment: ₹$amountStr"))
                } else {
                    sendEffect(MainEffect.ShowToast("No recent payment found to repeat"))
                }
            }
        }
    }
    
    fun sendEffect(effect: MainEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
