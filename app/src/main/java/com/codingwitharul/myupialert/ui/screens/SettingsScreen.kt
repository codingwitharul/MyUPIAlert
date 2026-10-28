package com.codingwitharul.myupialert.ui.screens

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.AutoStartHelper
import com.codingwitharul.myupialert.ForegroundTtsService
import com.codingwitharul.myupialert.R
import com.codingwitharul.myupialert.ui.MainEffect
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val palette = MaterialTheme.appPalette

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Voice, Permissions & Forwarding",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = palette.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background.copy(alpha = 0.95f),
                    titleContentColor = palette.textPrimary
                )
            )
        },
        containerColor = palette.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(colors = palette.backgroundGradient)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 0: Appearance & Theme (Day / Night Mode)
                GlassSectionCard(title = "Appearance & Theme", icon = "🎨") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isDarkMode) "Night Mode (Dark)" else "Day Mode (Light)",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.isDarkMode) "Cosmic obsidian glassmorphism" else "High-contrast slate for sunny counter displays",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = !state.isDarkMode,
                            onCheckedChange = { isDay ->
                                onIntent(MainIntent.ToggleTheme(!isDay))
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (!state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (!state.isDarkMode) Warning else Primary
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Warning,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = palette.surfaceVariant
                            )
                        )
                    }
                }

                // Section 1: Voice & Speech Engine
                GlassSectionCard(title = "Voice & Announcements", icon = "🎙️") {
                    // Language Dropdown
                    Text(
                        text = stringResource(R.string.language),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    var expandedLanguage by remember { mutableStateOf(false) }
                    val languages = listOf(
                        "English", "Hindi", "Tamil", "Telugu", "Kannada", 
                        "Malayalam", "Marathi", "Gujarati", "Bengali", 
                        "Punjabi", "Odia", "Urdu", "Assamese"
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = state.language,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = GlassSurface,
                                unfocusedContainerColor = GlassSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguage,
                            onDismissRequest = { expandedLanguage = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = TextPrimary) },
                                    onClick = {
                                        onIntent(MainIntent.ChangeLanguage(lang))
                                        expandedLanguage = false
                                        val langCode = when (lang.lowercase(java.util.Locale.ROOT)) {
                                            "hindi" -> "hi"
                                            "gujarati" -> "gu"
                                            "marathi" -> "mr"
                                            "tamil" -> "ta"
                                            "telugu" -> "te"
                                            "kannada" -> "kn"
                                            "malayalam" -> "ml"
                                            "punjabi" -> "pa"
                                            "bengali" -> "bn"
                                            "odia" -> "or"
                                            "assamese" -> "as"
                                            "urdu" -> "ur"
                                            else -> "en"
                                        }
                                        com.codingwitharul.myupialert.LocaleHelper.setAppLanguage(context, langCode, recreateActivity = true)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.volume),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            if (state.volume > 100) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔥 Boosted",
                                    fontSize = 11.sp,
                                    color = Warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${state.volume}%",
                            color = PrimaryLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = { onIntent(MainIntent.ChangeVolume(it.toInt())) },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryLight,
                            activeTrackColor = Primary,
                            inactiveTrackColor = GlassSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speech Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.speech_speed),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1fx", state.speedFloat),
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.speedInt.toFloat(),
                        onValueChange = {
                            val newFloat = 0.5f + (it / 10f)
                            onIntent(MainIntent.ChangeSpeed(it.toInt(), newFloat))
                        },
                        valueRange = 0f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan,
                            inactiveTrackColor = GlassSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Announce Sender Name Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.announce_sender),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.announce_sender_subtitle),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.announceSenderName,
                            onCheckedChange = { onIntent(MainIntent.ToggleAnnounceSender(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Success,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Store / Shop Name
                    Text(
                        text = "Store / Business Name",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = state.storeName,
                        onValueChange = { onIntent(MainIntent.ChangeStoreName(it)) },
                        placeholder = { Text("e.g. Nanotricks Store", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "Customizes announcement: '<Store Name>: Received ₹500 on PhonePe'",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onIntent(MainIntent.ResetOnboarding) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Re-Run Initial Device Setup Wizard",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pre-Announcement Chime Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Announcement Chime",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Plays instant crystal bell chime sound before speech",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.playChimeBeforeVoice,
                            onCheckedChange = { onIntent(MainIntent.ToggleChime(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Success,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strobe Camera Light Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Flash Camera Light on Alert",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Blinks flashlight 3 times for noisy market environments",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.strobeFlashlightOnPayment,
                            onCheckedChange = { onIntent(MainIntent.ToggleStrobeFlashlight(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Warning,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Duplicate Detection Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Detect Duplicate Payments",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Warns if a customer tries to replay an old SMS or screenshot with matching UTR",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.detectDuplicateTransactions,
                            onCheckedChange = { onIntent(MainIntent.ToggleDetectDuplicates(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sticky Status Bar Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sticky Status Bar & Live HUD",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Shows live soundbox status, today's collection total, last payment ticker, and quick controls pinned in the app and Android status bar",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.isStickyStatusBarEnabled,
                            onCheckedChange = {
                                onIntent(MainIntent.ToggleStickyStatusBar(it))
                                ForegroundTtsService.updateNotification(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Success,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Keep Screen Awake in Counter Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Screen Awake in Counter Mode",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Keeps display on when in Soundbox Counter display",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.soundboxKeepScreenOn,
                            onCheckedChange = { onIntent(MainIntent.ToggleSoundboxKeepScreenOn(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth Keep-Alive Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bluetooth Speaker Keep-Alive",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Prevents external Bluetooth speakers from entering power-save sleep mode",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.bluetoothKeepAlive,
                            onCheckedChange = { onIntent(MainIntent.ToggleBluetoothKeepAlive(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentViolet,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mute Announcements Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.mute_announcements),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.mute_subtitle),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.isMuted,
                            onCheckedChange = { onIntent(MainIntent.ToggleMute(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Error,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceLight
                            )
                        )
                    }
                }

                // Section 2: Sound Testing
                GlassSectionCard(title = "Test Sounds", icon = "🔊") {
                    Text(
                        text = "Preview announcements with your selected volume and speed settings.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onIntent(MainIntent.TestSound(amount = null)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.test_default_sound),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var testAmount by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testAmount,
                            onValueChange = { testAmount = it },
                            placeholder = { Text("e.g. 1500", color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = GlassSurface,
                                unfocusedContainerColor = GlassSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = if (testAmount.isNotBlank()) testAmount else "1500.00"
                                onIntent(MainIntent.TestSound(amount = amt))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.test_amount), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section 3: SMS Forwarder
                GlassSectionCard(title = stringResource(R.string.sms_forwarder), icon = "📲") {
                    Text(
                        text = "Automatically relay incoming transactional SMS to another number.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.forwarderNumber,
                        onValueChange = { onIntent(MainIntent.ChangeForwarderNumber(it)) },
                        placeholder = { Text(stringResource(R.string.forward_to_number), color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment Type Dropdown
                    Text(
                        text = stringResource(R.string.forward_payment_type),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    var expandedForwardType by remember { mutableStateOf(false) }
                    val forwardTypes = listOf("Both", "Credited", "Debited")
                    ExposedDropdownMenuBox(
                        expanded = expandedForwardType,
                        onExpandedChange = { expandedForwardType = !expandedForwardType },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = state.forwarderTypeFilter,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForwardType) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = GlassSurface,
                                unfocusedContainerColor = GlassSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedForwardType,
                            onDismissRequest = { expandedForwardType = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            forwardTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = TextPrimary) },
                                    onClick = {
                                        onIntent(MainIntent.ChangeForwarderType(type))
                                        expandedForwardType = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Forward App Dropdown
                    Text(
                        text = stringResource(R.string.forward_app),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    var expandedForwardApp by remember { mutableStateOf(false) }
                    val forwardApps = listOf("all", "Google Pay", "PhonePe", "Paytm")
                    ExposedDropdownMenuBox(
                        expanded = expandedForwardApp,
                        onExpandedChange = { expandedForwardApp = !expandedForwardApp },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = if (state.forwarderAppFilter == "all") "All Apps" else state.forwarderAppFilter,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForwardApp) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = GlassSurface,
                                unfocusedContainerColor = GlassSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedForwardApp,
                            onDismissRequest = { expandedForwardApp = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            forwardApps.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text(if (app == "all") "All Apps" else app, color = TextPrimary) },
                                    onClick = {
                                        onIntent(MainIntent.ChangeForwarderApp(app))
                                        expandedForwardApp = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 4: System Permissions & Longevity
                GlassSectionCard(title = "Permissions & Background Health", icon = "🛡️") {
                    // Notification Listener
                    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    val notifGranted = enabledListeners != null && enabledListeners.contains(context.packageName)

                    PermissionStatusRow(
                        title = "Notification Listener",
                        subtitle = "Required to detect GPay, PhonePe, Paytm alerts",
                        isGranted = notifGranted,
                        onAction = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Battery Optimization
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(context.packageName)

                    PermissionStatusRow(
                        title = "Unrestricted Battery Mode",
                        subtitle = "Prevents Android from stopping voice alerts when phone is locked",
                        isGranted = batteryIgnored,
                        onAction = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-start manager
                    OutlinedButton(
                        onClick = {
                            val autoStartIntent = AutoStartHelper.getAutoStartIntent(context)
                            if (autoStartIntent != null) {
                                try {
                                    context.startActivity(autoStartIntent)
                                } catch (e: Exception) {
                                    // fall through
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open OEM Auto-Start Settings (MIUI, Vivo, Oppo)", color = TextPrimary)
                    }
                }

                // Section 5: Statement & Data Export
                GlassSectionCard(title = "Data & Statements", icon = "📊") {
                    Button(
                        onClick = { onIntent(MainIntent.ExportTransactions) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_statement),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export HTML Statement", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onIntent(MainIntent.ExportCsv) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Success.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export CSV (Excel / Tally)", color = Success, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Section 6: About App
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MyUPIAlert v2.0 Gold Edition",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "100% Offline • Private • Zero Cloud Sync",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GlassSectionCard(
    title: String,
    icon: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.appPalette
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Text(text = icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }
            content()
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Success else Warning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    fontSize = 13.sp
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 22.dp)
            )
        }
        if (!isGranted) {
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryLight)
            ) {
                Text("Enable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
