package com.codingwitharul.myupialert.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.codingwitharul.myupialert.AutoStartHelper
import com.codingwitharul.myupialert.ForegroundTtsService
import com.codingwitharul.myupialert.R
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.ui.MainEffect
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.MainViewModel
import com.codingwitharul.myupialert.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Navigation state: Dashboard vs Dedicated Soundbox screen
    var currentScreen by rememberSaveable { mutableStateOf("dashboard") }
    var selectedTab by rememberSaveable { mutableStateOf("home") }

    // Dialog state for missing permissions
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.handleIntent(MainIntent.SetPermissionsGranted(allGranted))
    }

    // Helper to evaluate and sync current permission states
    val checkAndSyncPermissions = {
        val permissionsToCheck = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToCheck.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val smsGranted = permissionsToCheck.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val notifGranted = enabledListeners != null && enabledListeners.contains(context.packageName)

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(context.packageName)

        viewModel.handleIntent(
            MainIntent.UpdatePermissionStatus(
                smsGranted = smsGranted,
                notificationListenerGranted = notifGranted,
                batteryIgnored = batteryIgnored
            )
        )

        val coreGranted = smsGranted && notifGranted
        if (!coreGranted && !state.smsPermissionGranted) {
            showPermissionDialog = true
        }
    }

    // Re-check whenever app opens or resumes from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAndSyncPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial check on launch
    LaunchedEffect(Unit) {
        checkAndSyncPermissions()

        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MainEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                }
                MainEffect.OpenAutoStartSettings -> {
                    val intent = AutoStartHelper.getAutoStartIntent(context)
                    if (intent != null) context.startActivity(intent)
                }
                MainEffect.OpenNotificationSettings -> {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                MainEffect.RequestBatteryOptimization -> {
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
                MainEffect.RequestPermissions -> {
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
                is MainEffect.Speak -> {
                    val smsParser = org.koin.java.KoinJavaComponent.getKoin().get<com.codingwitharul.myupialert.smsparser.SmsParser>()
                    val lang = state.language
                    val announceSender = state.announceSenderName

                    val phrase = if (effect.phrase.contains("|") || effect.phrase.startsWith("Warning") || effect.phrase.startsWith("Today")) {
                        effect.phrase
                    } else {
                        smsParser.getAmountFromMessageBody(context, effect.phrase, lang, announceSender, "Test User")
                    }

                    val svc = Intent(context, ForegroundTtsService::class.java).apply {
                        action = ForegroundTtsService.ACTION_SPEAK
                        putExtra(ForegroundTtsService.EXTRA_TEXT, phrase)
                        putExtra(ForegroundTtsService.EXTRA_FORCE_SPEAK, effect.isForce)
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(svc)
                        } else {
                            context.startService(svc)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error starting TTS service: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is MainEffect.ExportHtml -> {
                    if (effect.transactions.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.toast_no_transactions), Toast.LENGTH_SHORT).show()
                        return@collectLatest
                    }
                    try {
                        val fileName = "UPI_Statement_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".html"
                        var fos: OutputStream? = null

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            }
                            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                fos = context.contentResolver.openOutputStream(uri)
                            }
                        } else {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = java.io.File(downloadsDir, fileName)
                            fos = java.io.FileOutputStream(file)
                        }

                        fos?.use { output ->
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta charset="utf-8">
                                    <title>UPI Statement</title>
                                    <style>
                                        body { font-family: -apple-system, sans-serif; background: #0b0f19; color: #f8fafc; padding: 20px; }
                                        h2 { color: #818cf8; }
                                        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                                        th, td { padding: 10px; border: 1px solid #1e293b; text-align: left; }
                                        th { background: #1e293b; color: #94a3b8; }
                                        .credit { color: #10b981; font-weight: bold; }
                                        .debit { color: #f43f5e; font-weight: bold; }
                                    </style>
                                </head>
                                <body>
                                    <h2>MyUPIAlert Statement</h2>
                                    <p>Exported: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}</p>
                                    <table>
                                        <tr>
                                            <th>Date</th>
                                            <th>Sender / App</th>
                                            <th>Type</th>
                                            <th>Amount</th>
                                            <th>Message</th>
                                        </tr>
                                        ${effect.transactions.joinToString("") { t ->
                                            val dateStr = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.US).format(Date(t.timestamp))
                                            val cls = if (t.type == "credit") "credit" else "debit"
                                            "<tr><td>$dateStr</td><td>${t.sender ?: "Unknown"}</td><td class='$cls'>${t.type ?: ""}</td><td class='$cls'>₹${t.amount ?: ""}</td><td>${t.body ?: ""}</td></tr>"
                                        }}
                                    </table>
                                </body>
                                </html>
                            """.trimIndent()
                            output.write(html.toByteArray(Charsets.UTF_8))
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.toast_export_failed, e.message), Toast.LENGTH_LONG).show()
                    }
                }
                is MainEffect.ExportCsvFile -> {
                    if (effect.transactions.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.toast_no_transactions), Toast.LENGTH_SHORT).show()
                        return@collectLatest
                    }
                    try {
                        val fileName = "UPI_Statement_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".csv"
                        var fos: OutputStream? = null

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            }
                            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                fos = context.contentResolver.openOutputStream(uri)
                            }
                        } else {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = java.io.File(downloadsDir, fileName)
                            fos = java.io.FileOutputStream(file)
                        }

                        fos?.use { output ->
                            val csvHeader = "ID,Timestamp,Date_Time,Sender,Type,Amount_INR,Reference_UTR,Message_Body\n"
                            output.write(csvHeader.toByteArray(Charsets.UTF_8))
                            for (t in effect.transactions) {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(t.timestamp))
                                val ref = extractReferenceNumber(t.body) ?: ""
                                val cleanBody = (t.body ?: "").replace("\"", "\"\"").replace("\n", " ")
                                val line = "\"${t.id}\",\"${t.timestamp}\",\"$dateStr\",\"${t.sender ?: ""}\",\"${t.type ?: ""}\",\"${t.amount ?: ""}\",\"$ref\",\"$cleanBody\"\n"
                                output.write(line.toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(context, "✅ CSV exported to Downloads!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "CSV export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    MyUPIAlertTheme(darkTheme = state.isDarkMode) {
        val palette = MaterialTheme.appPalette

        when (currentScreen) {
            "soundbox" -> {
                BackHandler { currentScreen = "dashboard" }
                SoundboxScreen(
                    state = state,
                    onIntent = { viewModel.handleIntent(it) },
                    onExitSoundbox = { currentScreen = "dashboard" }
                )
            }
            else -> {
                // MAIN EXECUTIVE DASHBOARD (Dual Day/Night Vibrant Glass UI)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(colors = palette.backgroundGradient)
                        )
                ) {
                    // Decorative glowing ambient blur gradients
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .offset(x = (-80).dp, y = (-60).dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Primary.copy(alpha = if (palette.isDark) 0.15f else 0.08f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 80.dp, y = 60.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Success.copy(alpha = if (palette.isDark) 0.12f else 0.06f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )

                    Scaffold(
                        topBar = {
                            if (selectedTab != "settings") {
                                GlassTopAppBar(
                                    state = state,
                                    onToggleTheme = { viewModel.handleIntent(MainIntent.ToggleTheme(!state.isDarkMode)) },
                                    onOpenSoundbox = { currentScreen = "soundbox" },
                                    onSpeakSummary = { viewModel.handleIntent(MainIntent.SpeakTodaySummary) },
                                    onRepeatLastPayment = { viewModel.handleIntent(MainIntent.RepeatLastPayment) },
                                    onExport = { viewModel.handleIntent(MainIntent.ExportTransactions) },
                                    onOpenDiagnostics = { showPermissionDialog = true },
                                    onOpenSettings = { selectedTab = "settings" }
                                )
                            }
                        },
                        bottomBar = {
                            GlassBottomNavBar(
                                selectedTab = selectedTab,
                                onSelectTab = { selectedTab = it }
                            )
                        },
                        containerColor = Color.Transparent
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            when (selectedTab) {
                                "home" -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 1. Persistent missing permissions banner if any are ungranted
                                        if (!state.allPermissionsGranted) {
                                            item(key = "permission_banner") {
                                                GlassPermissionBanner(
                                                    onGrantClicked = { showPermissionDialog = true }
                                                )
                                            }
                                        }

                                        // 2. High-End Executive Hero Collection Card
                                        item(key = "hero_collection") {
                                            HeroCollectionCard(
                                                transactions = state.transactions,
                                                dateLabel = state.dateFilterLabel,
                                                onRepeatLast = { viewModel.handleIntent(MainIntent.RepeatLastPayment) }
                                            )
                                        }

                                        // 3. Touch-Friendly Executive Quick Action Hub (4 Tiles)
                                        item(key = "quick_actions") {
                                            QuickActionsHub(
                                                onRepeatLast = { viewModel.handleIntent(MainIntent.RepeatLastPayment) },
                                                onOpenSoundbox = { currentScreen = "soundbox" },
                                                onSpeakSummary = { viewModel.handleIntent(MainIntent.SpeakTodaySummary) },
                                                onExport = { viewModel.handleIntent(MainIntent.ExportTransactions) }
                                            )
                                        }

                                        // 4. Filter Row (App and Date Glass Dropdowns)
                                        item(key = "filters") {
                                            FilterSection(
                                                state = state,
                                                onIntent = { viewModel.handleIntent(it) }
                                            )
                                        }

                                        // Note: HourlySalesAnalyticsCard is MOVED OUT OF HOME into the dedicated Analytics tab!

                                        // 5. Recent Transactions Section Header
                                        item(key = "recent_header") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Recent Transactions",
                                                    color = palette.textPrimary,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${state.transactions.size} records",
                                                    color = palette.textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // 6. Transaction Items
                                        if (state.transactions.isEmpty()) {
                                            item(key = "empty_transactions") {
                                                EmptyTransactionsView()
                                            }
                                        } else {
                                            items(
                                                items = state.transactions,
                                                key = { it.id }
                                            ) { transaction ->
                                                CollapsibleTransactionCard(transaction = transaction)
                                            }
                                        }
                                    }
                                }
                                "analytics" -> {
                                    AnalyticsScreen(
                                        state = state,
                                        onIntent = { viewModel.handleIntent(it) }
                                    )
                                }
                                "service" -> {
                                    ServiceControlScreen(
                                        state = state,
                                        onIntent = { viewModel.handleIntent(it) },
                                        onRequestPermissions = { showPermissionDialog = true }
                                    )
                                }
                                "settings" -> {
                                    SettingsScreen(
                                        state = state,
                                        onIntent = { viewModel.handleIntent(it) },
                                        onNavigateBack = { selectedTab = "home" }
                                    )
                                }
                            }
                        }
                    }

                    // Always show Permission Dialog on App Open if permissions are not granted
                    if (showPermissionDialog && !state.allPermissionsGranted) {
                        AlwaysShowPermissionDialog(
                            state = state,
                            onDismiss = { showPermissionDialog = false },
                            onRequestSms = { viewModel.sendEffect(MainEffect.RequestPermissions) },
                            onOpenNotificationSettings = { viewModel.sendEffect(MainEffect.OpenNotificationSettings) },
                            onRequestBatteryOptimization = { viewModel.sendEffect(MainEffect.RequestBatteryOptimization) }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Top App Bar with Glass UI, Live Monitoring Badge & More Overflow Menu
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    state: MainState,
    onToggleTheme: () -> Unit,
    onOpenSoundbox: () -> Unit,
    onSpeakSummary: () -> Unit,
    onRepeatLastPayment: () -> Unit,
    onExport: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(palette.primaryGradient),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = palette.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when {
                            !state.allPermissionsGranted -> Warning
                            !state.isServiceEnabled -> Error
                            !state.isServiceActiveNow -> Warning
                            else -> Success
                        }
                        val statusText = when {
                            !state.allPermissionsGranted -> "SETUP NEEDED"
                            !state.isServiceEnabled -> "SERVICE PAUSED"
                            !state.isServiceActiveNow -> "OUTSIDE HOURS"
                            else -> "LIVE MONITORING"
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        },
        actions = {
            // Day / Night Mode Toggle
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(38.dp)
                    .background(palette.glassSurface, shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (state.isDarkMode) "Switch to Day Mode" else "Switch to Night Mode",
                    tint = if (state.isDarkMode) Warning else Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // More Overflow Menu Button (Satisfies "Add more menu required")
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(palette.glassSurface, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.background(palette.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("🔊 Counter Soundbox", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Storefront, null, tint = Primary, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onOpenSoundbox()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📢 Speak Today's Summary", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = AccentCyan, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onSpeakSummary()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔁 Repeat Last Payment", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Replay, null, tint = Success, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onRepeatLastPayment()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📥 Export Statement", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = Warning, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onExport()
                        }
                    )
                    HorizontalDivider(color = palette.glassBorder)
                    DropdownMenuItem(
                        text = { Text("🛡️ Permission Diagnostics", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Shield, null, tint = AccentViolet, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onOpenDiagnostics()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚙️ Settings", color = palette.textPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            onOpenSettings()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = palette.textPrimary
        )
    )
}

// -----------------------------------------------------------------------------
// Executive Glass Bottom Navigation Bar
// -----------------------------------------------------------------------------
@Composable
fun GlassBottomNavBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit
) {
    val palette = MaterialTheme.appPalette
    NavigationBar(
        containerColor = palette.surface.copy(alpha = if (palette.isDark) 0.95f else 0.98f),
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = { onSelectTab("home") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Home",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == "home") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "analytics",
            onClick = { onSelectTab("analytics") },
            icon = {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Analytics",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Analytics",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == "analytics") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "service",
            onClick = { onSelectTab("service") },
            icon = {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Service",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Service",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == "service") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "settings",
            onClick = { onSelectTab("settings") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Settings",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == "settings") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            )
        )
    }
}

// -----------------------------------------------------------------------------
// Prominent Permission Banner on Dashboard
// -----------------------------------------------------------------------------
@Composable
fun GlassPermissionBanner(onGrantClicked: () -> Unit) {
    val palette = MaterialTheme.appPalette
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (palette.isDark) Color(0x28F59E0B) else Color(0x1AF59E0B)),
        border = BorderStroke(1.dp, Color(0x66F59E0B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Permissions Incomplete",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "App needs SMS & notification access to announce payments",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Warning),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Grant Now",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// "Grant permission allways show when app opens" Dialog
// -----------------------------------------------------------------------------
@Composable
fun AlwaysShowPermissionDialog(
    state: MainState,
    onDismiss: () -> Unit,
    onRequestSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            border = BorderStroke(1.5.dp, if (palette.isDark) GlassBorderPrimary else Primary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(palette.primaryGradient),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Permissions Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MyUPIAlert runs 100% offline. Grant these 3 permissions so payments are announced instantly.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Item 1: SMS Access
                PermissionItemCard(
                    title = "1. SMS & Push Alerts",
                    description = "Intercept bank transaction SMS to announce credits",
                    isGranted = state.smsPermissionGranted,
                    actionLabel = "Grant SMS",
                    onAction = onRequestSms
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Item 2: Notification Listener
                PermissionItemCard(
                    title = "2. Notification Listener",
                    description = "Detect payment alerts from PhonePe, GPay, Paytm",
                    isGranted = state.notificationListenerGranted,
                    actionLabel = "Enable Access",
                    onAction = onOpenNotificationSettings
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Item 3: Battery Optimization Whitelist
                PermissionItemCard(
                    title = "3. Unrestricted Battery",
                    description = "Keeps voice announcement engine alive when screen is off",
                    isGranted = state.batteryOptimizationIgnored,
                    actionLabel = "Whitelist App",
                    onAction = onRequestBatteryOptimization
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom Action Button
                Button(
                    onClick = {
                        if (!state.smsPermissionGranted) {
                            onRequestSms()
                        } else if (!state.notificationListenerGranted) {
                            onOpenNotificationSettings()
                        } else if (!state.batteryOptimizationIgnored) {
                            onRequestBatteryOptimization()
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.allPermissionsGranted) Success else Primary
                    )
                ) {
                    Text(
                        text = if (state.allPermissionsGranted) "Ready! Continue to App" else "Setup Remaining Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                if (!state.allPermissionsGranted) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Skip for Now",
                            color = palette.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) (if (palette.isDark) Color(0x1A10B981) else Color(0x1410B981)) else palette.glassSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isGranted) Color(0x6610B981) else palette.glassBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isGranted) {
                Text(
                    text = "Active",
                    color = Success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Filter Section (App & Date Dropdowns)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(state: MainState, onIntent: (MainIntent) -> Unit) {
    val palette = MaterialTheme.appPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Filter Dropdown
        var appExpanded by remember { mutableStateOf(false) }
        val apps = listOf("all", "Google Pay", "PhonePe", "Paytm", "CRED", "Amazon Pay")
        ExposedDropdownMenuBox(
            expanded = appExpanded,
            onExpandedChange = { appExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = if (state.appFilter == "all") "All Apps" else state.appFilter,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appExpanded) },
                modifier = Modifier.menuAnchor(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = palette.glassBorder,
                    focusedContainerColor = palette.glassSurface,
                    unfocusedContainerColor = palette.glassSurface,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )
            )
            ExposedDropdownMenu(
                expanded = appExpanded,
                onDismissRequest = { appExpanded = false },
                modifier = Modifier.background(palette.surface)
            ) {
                apps.forEach { app ->
                    DropdownMenuItem(
                        text = { Text(if (app == "all") "All Apps" else app, color = palette.textPrimary, fontSize = 13.sp) },
                        onClick = {
                            onIntent(MainIntent.ChangeAppFilter(app))
                            appExpanded = false
                        }
                    )
                }
            }
        }

        // Date Filter Dropdown
        var dateExpanded by remember { mutableStateOf(false) }
        val dates = listOf("Today", "Yesterday", "Last 7 Days", "This Month", "All Time")
        ExposedDropdownMenuBox(
            expanded = dateExpanded,
            onExpandedChange = { dateExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = state.dateFilterLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                modifier = Modifier.menuAnchor(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = palette.glassBorder,
                    focusedContainerColor = palette.glassSurface,
                    unfocusedContainerColor = palette.glassSurface,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )
            )
            ExposedDropdownMenu(
                expanded = dateExpanded,
                onDismissRequest = { dateExpanded = false },
                modifier = Modifier.background(palette.surface)
            ) {
                dates.forEach { dateLabel ->
                    DropdownMenuItem(
                        text = { Text(dateLabel, color = palette.textPrimary, fontSize = 13.sp) },
                        onClick = {
                            val range = getDateRange(dateLabel)
                            onIntent(MainIntent.ChangeDateFilter(range.first, range.second, dateLabel))
                            dateExpanded = false
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
@Composable
fun HeroCollectionCard(
    transactions: List<PaymentTransaction>,
    dateLabel: String,
    onRepeatLast: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
    val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
    val creditCount = transactions.count { it.type == "credit" }
    val lastCredit = transactions.firstOrNull { it.type == "credit" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (palette.isDark) {
                            listOf(Color(0xFF0F172A), Color(0xFF131D31), Color(0xFF090D16))
                        } else {
                            listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFEDF2F7))
                        }
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Success, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dateLabel.equals("Today", ignoreCase = true)) "TODAY'S COLLECTION" else "$dateLabel COLLECTION".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = palette.textSecondary
                        )
                    }

                    // Count Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Success.copy(alpha = if (palette.isDark) 0.15f else 0.12f),
                        border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$creditCount Received",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (palette.isDark) Success else SuccessDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Large Authoritative Rupee Typography
                Text(
                    text = String.format(Locale.US, "₹%,.2f", totalCredit),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.textPrimary,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-metrics row: Inflow vs Outflow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = palette.glassSurface,
                        border = BorderStroke(1.dp, palette.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Inflow", fontSize = 10.sp, color = palette.textMuted)
                                Text(
                                    String.format(Locale.US, "₹%.0f", totalCredit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = palette.glassSurface,
                        border = BorderStroke(1.dp, palette.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Outflow", fontSize = 10.sp, color = palette.textMuted)
                                Text(
                                    String.format(Locale.US, "₹%.0f", totalDebit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }
                }

                // Quick Repeat Pill if last payment exists
                if (lastCredit != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = onRepeatLast,
                        shape = RoundedCornerShape(12.dp),
                        color = Primary.copy(alpha = if (palette.isDark) 0.12f else 0.08f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Repeat",
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Last: ₹${lastCredit.amount ?: "0"} received",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                            }
                            Text(
                                text = "🔊 Tap to Repeat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Executive Quick Action Hub: 4 Touch-Friendly Tiles
// -----------------------------------------------------------------------------
@Composable
fun QuickActionsHub(
    onRepeatLast: () -> Unit,
    onOpenSoundbox: () -> Unit,
    onSpeakSummary: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionTile(
            title = "Repeat Last",
            subtitle = "Loud Voice",
            icon = Icons.Default.Replay,
            gradient = listOf(SuccessDark, Success),
            modifier = Modifier.weight(1f),
            onClick = onRepeatLast
        )
        QuickActionTile(
            title = "Soundbox",
            subtitle = "Live Display",
            icon = Icons.Default.Storefront,
            gradient = listOf(PrimaryDark, AccentViolet),
            modifier = Modifier.weight(1f),
            onClick = onOpenSoundbox
        )
        QuickActionTile(
            title = "Summary",
            subtitle = "Voice Brief",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            gradient = listOf(Color(0xFF0284C7), AccentCyan),
            modifier = Modifier.weight(1f),
            onClick = onSpeakSummary
        )
        QuickActionTile(
            title = "Statement",
            subtitle = "CSV / HTML",
            icon = Icons.Default.FileDownload,
            gradient = listOf(Color(0xFFD97706), Warning),
            modifier = Modifier.weight(1f),
            onClick = onExport
        )
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.glassCard,
        border = BorderStroke(1.dp, palette.glassBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Brush.linearGradient(gradient), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

fun getDateRange(range: String): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    var start = 0L
    var end = Long.MAX_VALUE
    when (range) {
        "Today" -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            end = cal.timeInMillis
        }
        "Yesterday" -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            end = cal.timeInMillis
        }
        "Last 7 Days" -> {
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            start = cal.timeInMillis

            val endCal = Calendar.getInstance()
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)
            end = endCal.timeInMillis
        }
        "This Month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            start = cal.timeInMillis

            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            end = cal.timeInMillis
        }
        "All Time" -> {
            start = 0L
            end = Long.MAX_VALUE
        }
        else -> {
            start = 0L
            end = Long.MAX_VALUE
        }
    }
    return Pair(start, end)
}

// -----------------------------------------------------------------------------
// Transaction List & Collapsible Transaction Item
// -----------------------------------------------------------------------------
@Composable
fun EmptyTransactionsView(modifier: Modifier = Modifier) {
    val palette = MaterialTheme.appPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💳",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No transactions found",
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Incoming UPI payment alerts will appear here in real time.",
                color = palette.textSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TransactionList(transactions: List<PaymentTransaction>, modifier: Modifier = Modifier) {
    if (transactions.isEmpty()) {
        EmptyTransactionsView(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                CollapsibleTransactionCard(transaction = transaction)
            }
        }
    }
}

/**
 * Collapsible Transaction Item:
 * - Collapsed State: Shows ONLY required info (App/Sender avatar, Name, Timestamp, Amount badge, expand chevron).
 * - Expanded State: On click, smoothly animates to reveal full information:
 *   - Reference / UTR Number with 1-tap Copy
 *   - Sender Package / Origin address
 *   - Exact Full Date and Timestamp
 *   - Full Raw Message Body with Copy Message button
 */
@Composable
fun CollapsibleTransactionCard(transaction: PaymentTransaction) {
    val palette = MaterialTheme.appPalette
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val isCredit = transaction.type == "credit"

    // Clean sender name & avatar styling
    val cleanSender = getCleanSenderName(transaction.sender)
    val appGradient = getAppGradient(cleanSender)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(
            1.dp,
            if (isExpanded) Primary else palette.glassBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // -----------------------------------------------------------------
            // REQUIRED INFORMATION ONLY (Always Visible Header)
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vibrant App / Bank Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Brush.linearGradient(appGradient), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getSenderMonogram(cleanSender),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Sender Name & Short Timestamp
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanSender,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val shortDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
                    Text(
                        text = shortDate,
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount & Pill Badge
                Column(horizontalAlignment = Alignment.End) {
                    val amountStr = if (isCredit) "+₹${transaction.amount}" else "-₹${transaction.amount}"
                    val amountColor = if (isCredit) Success else Error
                    Text(
                        text = amountStr,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isCredit) Color(0x2410B981) else Color(0x24F43F5E),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCredit) "CREDIT" else "DEBIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCredit) Success else Error
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Smoothly Rotating Expand Chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand for full details",
                    tint = palette.textMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            // -----------------------------------------------------------------
            // FULL INFORMATION (Shown only when clicked / expanded)
            // -----------------------------------------------------------------
            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = palette.glassBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Extracted Reference / UTR Number
                val utr = extractReferenceNumber(transaction.body)
                if (!utr.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reference / UTR",
                                fontSize = 11.sp,
                                color = palette.textSecondary
                            )
                            Text(
                                text = utr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "UTR", utr)
                                Toast.makeText(context, "Copied UTR: $utr", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UTR",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Origin / Package Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source ID / Package",
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                    Text(
                        text = transaction.sender ?: "Unknown",
                        fontSize = 12.sp,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                }

                // Full Exact Timestamp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Exact Time",
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                    val fullDate = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(transaction.timestamp))
                    Text(
                        text = fullDate,
                        fontSize = 11.sp,
                        color = palette.textPrimary
                    )
                }

                // Full Raw Message Body Sub-Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.glassSurface),
                    border = BorderStroke(1.dp, palette.glassBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAW MESSAGE BODY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textMuted
                            )
                            IconButton(
                                onClick = {
                                    val body = transaction.body ?: ""
                                    copyToClipboard(context, "UPI Message", body)
                                    Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Full Message",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.body ?: "No message content",
                            fontSize = 12.sp,
                            color = palette.textPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helpers: Clean sender naming, monograms, gradient colors, clipboard & regex
// -----------------------------------------------------------------------------
fun getCleanSenderName(sender: String?): String {
    if (sender.isNullOrBlank()) return "UPI Transaction"
    val lower = sender.lowercase(Locale.ROOT)
    return when {
        sender.contains("com.google.android.apps.nbu.paisa.user") || lower.contains("gpay") || lower.contains("google pay") -> "Google Pay"
        sender.contains("com.phonepe.app") || lower.contains("phonepe") -> "PhonePe"
        sender.contains("net.one97.paytm") || lower.contains("paytm") -> "Paytm"
        sender.contains("com.dreamplug.androidapp") || lower.contains("cred") -> "CRED"
        sender.contains("in.amazon.mShop.android.shopping") || lower.contains("amazon") -> "Amazon Pay"
        sender.contains("in.org.npci.upiapp") || lower.contains("bhim") -> "BHIM UPI"
        sender.contains("com.bharatpe.app") || lower.contains("bharatpe") -> "BharatPe"
        sender.contains("com.fampay.in") || lower.contains("fampay") || lower.contains("#fampaid") -> "FamPay"
        lower.contains("hdfc") -> "HDFC Bank"
        lower.contains("sbi") -> "SBI Bank"
        lower.contains("icici") -> "ICICI Bank"
        lower.contains("axis") -> "Axis Bank"
        lower.contains("pnb") -> "PNB Bank"
        lower.contains("canara") -> "Canara Bank"
        lower.contains("bob") || lower.contains("baroda") -> "Bank of Baroda"
        lower.contains("kotak") -> "Kotak Bank"
        lower.contains("indusind") -> "IndusInd Bank"
        lower.contains("union") -> "Union Bank"
        else -> sender
    }
}

fun getSenderMonogram(name: String): String {
    return when {
        name.startsWith("Google") -> "GP"
        name.startsWith("Phone") -> "PE"
        name.startsWith("Paytm") -> "PT"
        name.startsWith("CRED") -> "CR"
        name.startsWith("Amazon") -> "AP"
        name.startsWith("BHIM") -> "BH"
        name.startsWith("FamPay") -> "FP"
        name.length >= 2 -> name.take(2).uppercase()
        else -> name.take(1).uppercase()
    }
}

fun getAppGradient(name: String): List<Color> {
    return when {
        name.contains("Google") -> listOf(Color(0xFF4285F4), Color(0xFF1A73E8))
        name.contains("PhonePe") -> listOf(Color(0xFF5F259F), Color(0xFF7E22CE))
        name.contains("Paytm") -> listOf(Color(0xFF002970), Color(0xFF00BAF2))
        name.contains("CRED") -> listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        name.contains("Amazon") -> listOf(Color(0xFFFF9900), Color(0xFFEA580C))
        name.contains("FamPay") -> listOf(Color(0xFFFF9800), Color(0xFFFF5722))
        name.contains("HDFC") -> listOf(Color(0xFF004C8F), Color(0xFF002A54))
        name.contains("SBI") -> listOf(Color(0xFF280071), Color(0xFF00A3E0))
        name.contains("ICICI") -> listOf(Color(0xFFB02A30), Color(0xFF8B1E23))
        name.contains("Axis") -> listOf(Color(0xFF97144D), Color(0xFF7B0F3E))
        else -> listOf(Primary, AccentViolet)
    }
}

fun extractReferenceNumber(body: String?): String? {
    if (body.isNullOrBlank()) return null
    val pattern = Pattern.compile("(?i)(?:upi ref|ref no|ref|utr|txn id|rrn)[:\\s-]*([0-9a-zA-Z]{6,})")
    val matcher = pattern.matcher(body)
    return if (matcher.find()) {
        matcher.group(1)
    } else null
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
