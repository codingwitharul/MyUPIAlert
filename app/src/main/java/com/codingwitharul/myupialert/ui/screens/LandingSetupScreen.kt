package com.codingwitharul.myupialert.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.codingwitharul.myupialert.AutoStartHelper
import com.codingwitharul.myupialert.ForegroundTtsService
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*

/**
 * Supported announcement languages with their native script representations.
 */
data class SetupLanguage(
    val key: String,
    val englishName: String,
    val nativeScript: String
)

val SETUP_LANGUAGES = listOf(
    SetupLanguage("English", "English", "Default"),
    SetupLanguage("Hindi", "Hindi", "हिन्दी"),
    SetupLanguage("Tamil", "Tamil", "தமிழ்"),
    SetupLanguage("Telugu", "Telugu", "తెలుగు"),
    SetupLanguage("Kannada", "Kannada", "ಕನ್ನಡ"),
    SetupLanguage("Malayalam", "Malayalam", "മലയാളം"),
    SetupLanguage("Marathi", "Marathi", "मराठी"),
    SetupLanguage("Gujarati", "Gujarati", "ગુજરાતી"),
    SetupLanguage("Bengali", "Bengali", "বাংলা"),
    SetupLanguage("Punjabi", "Punjabi", "ਪੰਜਾਬੀ"),
    SetupLanguage("Odia", "Odia", "ଓଡ଼ିଆ"),
    SetupLanguage("Assamese", "Assamese", "অসমীয়া"),
    SetupLanguage("Urdu", "Urdu", "اردو")
)

/**
 * Landing Page & Device Setup Wizard — MyUPIAlert
 *
 * Runs automatically on first app start to configure:
 * 1. Business / Store identity
 * 2. Voice announcement language + live TTS audio test
 * 3. Required permissions (Notification Listener, Battery optimization, SMS backup)
 * 4. Soundbox loudness and chime preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingSetupScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onFinishSetup: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val palette = MaterialTheme.appPalette
    val scrollState = rememberScrollState()

    // Form inputs state
    var storeName by remember { mutableStateOf(state.storeName) }
    var selectedLanguage by remember { mutableStateOf(state.language.ifBlank { "English" }) }
    var maxVolumeBoost by remember { mutableStateOf(true) }
    var playChime by remember { mutableStateOf(state.playChimeBeforeVoice) }
    var announceSender by remember { mutableStateOf(state.announceSenderName) }

    // Live permission statuses
    var hasNotifListener by remember { mutableStateOf(false) }
    var isBatteryIgnored by remember { mutableStateOf(false) }
    var hasSmsPermission by remember { mutableStateOf(false) }
    var showNotifWarningDialog by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        hasNotifListener = enabledListeners != null && enabledListeners.contains(context.packageName)

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(context.packageName)

        val smsCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        hasSmsPermission = smsCheck
    }

    // Refresh permissions on initial compose and on resume from settings
    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // SMS permission request launcher
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasSmsPermission = result[Manifest.permission.RECEIVE_SMS] == true
        if (hasSmsPermission) {
            Toast.makeText(context, "Bank SMS backup enabled ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun completeAndLaunch() {
        onIntent(
            MainIntent.CompleteOnboarding(
                storeName = storeName.trim(),
                language = selectedLanguage,
                announceSender = announceSender,
                playChime = playChime,
                maxVolumeBoost = maxVolumeBoost
            )
        )
        // Ensure foreground TTS service is running
        try {
            val serviceIntent = Intent(context, ForegroundTtsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service startup fallback
        }
        onFinishSetup()
    }

    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            Surface(
                color = palette.surface,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, palette.glassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasNotifListener) {
                                showNotifWarningDialog = true
                            } else {
                                completeAndLaunch()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Complete Setup & Launch Soundbox",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // =================================================================
            // HERO / LANDING BANNER
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Primary.copy(alpha = 0.25f),
                                AccentViolet.copy(alpha = 0.15f),
                                palette.surface
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Primary.copy(alpha = 0.6f), AccentCyan.copy(alpha = 0.3f))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Animated Soundbox Emblem
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                Brush.linearGradient(listOf(Primary, AccentViolet)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "MyUPIAlert Soundbox",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Transform this smartphone into an intelligent, loud UPI voice alert device with zero monthly fees.",
                        fontSize = 13.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benefit Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BenefitPill(text = "₹0 Monthly Fee", color = Success)
                        BenefitPill(text = "100% Offline & Private", color = AccentCyan)
                        BenefitPill(text = "13 Indian Languages", color = AccentViolet)
                        BenefitPill(text = "Smart Fraud Shield", color = Warning)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =================================================================
            // SECTION 1: MERCHANT / STORE IDENTITY
            // =================================================================
            SectionCard(
                title = "1. Store & Merchant Identity",
                subtitle = "Announced in payment alerts & branded on customer receipts",
                palette = palette
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Shop / Business Name") },
                    placeholder = { Text("e.g. Ramesh Tea Stall, Sharma Kirana") },
                    leadingIcon = {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Primary)
                    },
                    trailingIcon = {
                        if (storeName.isNotBlank()) {
                            IconButton(onClick = { storeName = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = palette.textMuted)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = palette.glassBorder,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        focusedContainerColor = palette.glassSurface,
                        unfocusedContainerColor = palette.glassSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions chips
                Text(
                    text = "Quick suggestions:",
                    fontSize = 11.sp,
                    color = palette.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf("General Store", "Tea Stall", "Cafe", "Supermarket", "Medical Store", "Personal")
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = { storeName = preset },
                            label = { Text(preset, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = palette.glassSurface,
                                labelColor = palette.textSecondary
                            ),
                            border = BorderStroke(1.dp, palette.glassBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // SECTION 2: VOICE ANNOUNCEMENT LANGUAGE
            // =================================================================
            SectionCard(
                title = "2. Voice Language & Audio Test",
                subtitle = "Choose your preferred Indian dialect for spoken alerts",
                palette = palette
            ) {
                var langDropdownExpanded by remember { mutableStateOf(false) }
                val currentLangObj = SETUP_LANGUAGES.firstOrNull { it.key.equals(selectedLanguage, ignoreCase = true) }
                    ?: SETUP_LANGUAGES.first()

                ExposedDropdownMenuBox(
                    expanded = langDropdownExpanded,
                    onExpandedChange = { langDropdownExpanded = !langDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${currentLangObj.englishName} (${currentLangObj.nativeScript})",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = AccentViolet)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = langDropdownExpanded)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = palette.glassBorder,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedContainerColor = palette.glassSurface,
                            unfocusedContainerColor = palette.glassSurface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false }
                    ) {
                        SETUP_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = lang.englishName,
                                            fontWeight = if (lang.key == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = lang.nativeScript,
                                            color = AccentCyan,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedLanguage = lang.key
                                    langDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test voice announcement button
                OutlinedButton(
                    onClick = {
                        val shop = if (storeName.isNotBlank()) "at $storeName" else ""
                        val testPhrase = "Testing soundbox! Received |100| rupees $shop on MyUPIAlert."
                        onIntent(MainIntent.ChangeLanguage(selectedLanguage))
                        onIntent(MainIntent.TestSound("100"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Test Voice",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play Test Voice Announcement (₹100)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // SECTION 3: WHAT IS REQUIRED MOST (CRITICAL PERMISSIONS)
            // =================================================================
            SectionCard(
                title = "3. Required Device Permissions",
                subtitle = "Essential for 24/7 background operation and instant payment capture",
                palette = palette
            ) {
                // Card A: Notification Listener (CRITICAL)
                PermissionSetupTile(
                    title = "UPI Notification Access",
                    subtitle = "Required to hear alerts from Google Pay, PhonePe, Paytm, BHIM, CRED, BharatPe, etc.",
                    isGranted = hasNotifListener,
                    isMandatory = true,
                    icon = Icons.Default.NotificationsActive,
                    actionText = "Enable Access",
                    palette = palette,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card B: Battery Saver Exclusion (CRITICAL FOR LONGEVITY)
                PermissionSetupTile(
                    title = "Battery Saver Exclusion",
                    subtitle = "Prevents Android from killing the soundbox service when the phone is locked.",
                    isGranted = isBatteryIgnored,
                    isMandatory = true,
                    icon = Icons.Default.Bolt,
                    actionText = "Allow Background",
                    palette = palette,
                    onAction = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (e2: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card C: Bank SMS Backup (OPTIONAL)
                PermissionSetupTile(
                    title = "Bank SMS Backup Alert",
                    subtitle = "Announces offline transactions when UPI app push notifications are missed.",
                    isGranted = hasSmsPermission,
                    isMandatory = false,
                    icon = Icons.Default.Sms,
                    actionText = "Grant SMS",
                    palette = palette,
                    onAction = {
                        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        smsLauncher.launch(perms.toTypedArray())
                    }
                )

                // Optional Auto-Start OEM helper
                val autoStartIntent = remember { AutoStartHelper.getAutoStartIntent(context) }
                if (autoStartIntent != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionSetupTile(
                        title = "OEM Auto-Start (Xiaomi/Vivo/Oppo)",
                        subtitle = "Enable Auto-start to ensure soundbox boots automatically when phone restarts.",
                        isGranted = false,
                        isMandatory = false,
                        icon = Icons.Default.Autorenew,
                        actionText = "Open Auto-Start",
                        palette = palette,
                        onAction = {
                            try {
                                context.startActivity(autoStartIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open manufacturer settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // SECTION 4: SOUNDBOX PREFERENCES
            // =================================================================
            SectionCard(
                title = "4. Audio & Alert Preferences",
                subtitle = "Tune loudness, chime, and customer announcements",
                palette = palette
            ) {
                // Max Volume Boost
                PreferenceToggleRow(
                    title = "Maximum Volume & Loudness Boost",
                    subtitle = "Optimized for busy and noisy shop counters",
                    checked = maxVolumeBoost,
                    onCheckedChange = { maxVolumeBoost = it },
                    palette = palette
                )

                HorizontalDivider(color = palette.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Chime Before Voice
                PreferenceToggleRow(
                    title = "Play Attention Chime Ding",
                    subtitle = "Crisp bell sound before announcing amount",
                    checked = playChime,
                    onCheckedChange = { playChime = it },
                    palette = palette
                )

                HorizontalDivider(color = palette.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Announce Customer Name
                PreferenceToggleRow(
                    title = "Announce Customer Name",
                    subtitle = "Speaks \"Received from Rahul\" when name is available",
                    checked = announceSender,
                    onCheckedChange = { announceSender = it },
                    palette = palette
                )

                HorizontalDivider(color = palette.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Sticky Status Bar HUD
                var stickyStatusEnabled by remember { mutableStateOf(state.isStickyStatusBarEnabled) }
                PreferenceToggleRow(
                    title = "Sticky Status Bar & Live HUD",
                    subtitle = "Displays live soundbox status, collection total, and ticker in app and status bar",
                    checked = stickyStatusEnabled,
                    onCheckedChange = {
                        stickyStatusEnabled = it
                        onIntent(MainIntent.ToggleStickyStatusBar(it))
                    },
                    palette = palette
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Confirmation dialog if notification permission was skipped
    if (showNotifWarningDialog) {
        AlertDialog(
            onDismissRequest = { showNotifWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Warning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notification Access Missing")
                }
            },
            text = {
                Text(
                    "Without Notification Access, MyUPIAlert cannot detect payments from Google Pay, PhonePe, or Paytm.\n\nWe strongly recommend enabling it now for your soundbox to work.",
                    fontSize = 14.sp,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotifWarningDialog = false
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Enable Access Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotifWarningDialog = false
                        completeAndLaunch()
                    }
                ) {
                    Text("Skip for Now", color = palette.textMuted)
                }
            },
            containerColor = palette.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// Sub-components: Cards, Benefit Pills, Permission Tiles & Toggle Rows
// -----------------------------------------------------------------------------

@Composable
private fun BenefitPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    palette: AppPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = palette.textSecondary,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun PermissionSetupTile(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    isMandatory: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String,
    palette: AppPalette,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Success.copy(alpha = 0.08f) else palette.glassSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isGranted) Success.copy(alpha = 0.35f) else palette.glassBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isGranted) Success.copy(alpha = 0.2f) else Primary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Success else Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    if (isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Error.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "REQUIRED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Box(
                    modifier = Modifier
                        .background(Success.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Active ✓",
                        color = Success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMandatory) Primary else palette.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isMandatory) Color.White else palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    palette: AppPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.glassSurface
            )
        )
    }
}
