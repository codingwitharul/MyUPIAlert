package com.codingwitharul.myupialert.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*
import java.util.Locale

@Composable
fun ServiceControlScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Service Control & Scheduled Active Hours (Unified Section)
        ServiceOperationsCard(
            state = state,
            onIntent = onIntent,
            context = context
        )

        // System Longevity & Background Health Diagnostics
        BackgroundHealthCard(
            state = state,
            onIntent = onIntent,
            onRequestPermissions = onRequestPermissions
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Unified Card: Stop/Start Service Manual Override + Scheduled Start and End Time in ONE Section.
 */
@Composable
fun ServiceOperationsCard(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    context: Context
) {
    val palette = MaterialTheme.appPalette
    val isRunning = state.isServiceActiveNow

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(
            1.5.dp,
            when {
                !state.isServiceEnabled -> Error.copy(alpha = 0.5f)
                !isRunning -> Warning.copy(alpha = 0.5f)
                else -> Success.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Live Status Header Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    !state.isServiceEnabled -> Error.copy(alpha = 0.12f)
                    !isRunning -> Warning.copy(alpha = 0.12f)
                    else -> Success.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        !state.isServiceEnabled -> Error.copy(alpha = 0.35f)
                        !isRunning -> Warning.copy(alpha = 0.35f)
                        else -> Success.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                when {
                                    !state.isServiceEnabled -> Error
                                    !isRunning -> Warning
                                    else -> Success
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !state.isServiceEnabled -> "SERVICE PAUSED (MANUAL OVERRIDE)"
                            !isRunning -> "SILENCED (OUTSIDE STORE HOURS)"
                            else -> "LIVE MONITORING ACTIVE"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            !state.isServiceEnabled -> Error
                            !isRunning -> Warning
                            else -> if (palette.isDark) Success else SuccessDark
                        },
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Part 1: Manual Override (Start / Stop Service)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Master Voice Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.isServiceEnabled)
                            "Voice announcements are enabled for incoming payments"
                        else
                            "Announcements manually halted by store operator",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = state.isServiceEnabled,
                    onCheckedChange = { onIntent(MainIntent.ToggleServiceEnabled(it)) },
                    thumbContent = {
                        Icon(
                            imageVector = if (state.isServiceEnabled) Icons.Default.PowerSettingsNew else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (state.isServiceEnabled) Success else Error
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Success,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = palette.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.glassBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Part 2: Scheduled Start and End Operating Hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Store Operating Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Auto-silence voice announcements during closed hours",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                Switch(
                    checked = state.isScheduleEnabled,
                    onCheckedChange = { onIntent(MainIntent.ToggleScheduleEnabled(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = palette.surfaceVariant
                    )
                )
            }

            // Time Pickers (Visible when schedule is enabled)
            if (state.isScheduleEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Time Selector
                    TimeSelectorTile(
                        label = "Store Opens",
                        timeStr = formatTimeString(state.scheduleStartHour, state.scheduleStartMinute),
                        icon = Icons.Default.WbSunny,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    onIntent(MainIntent.UpdateScheduleTime(h, m, state.scheduleEndHour, state.scheduleEndMinute))
                                },
                                state.scheduleStartHour,
                                state.scheduleStartMinute,
                                false
                            ).show()
                        }
                    )

                    // End Time Selector
                    TimeSelectorTile(
                        label = "Store Closes",
                        timeStr = formatTimeString(state.scheduleEndHour, state.scheduleEndMinute),
                        icon = Icons.Default.Bedtime,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    onIntent(MainIntent.UpdateScheduleTime(state.scheduleStartHour, state.scheduleStartMinute, h, m))
                                },
                                state.scheduleEndHour,
                                state.scheduleEndMinute,
                                false
                            ).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Schedule Quick Presets
                Text(
                    text = "QUICK PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SchedulePresetChip(
                        label = "9AM-9PM",
                        selected = state.scheduleStartHour == 9 && state.scheduleEndHour == 21,
                        onClick = { onIntent(MainIntent.UpdateScheduleTime(9, 0, 21, 0)) },
                        modifier = Modifier.weight(1f)
                    )
                    SchedulePresetChip(
                        label = "8AM-10PM",
                        selected = state.scheduleStartHour == 8 && state.scheduleEndHour == 22,
                        onClick = { onIntent(MainIntent.UpdateScheduleTime(8, 0, 22, 0)) },
                        modifier = Modifier.weight(1f)
                    )
                    SchedulePresetChip(
                        label = "10AM-11PM",
                        selected = state.scheduleStartHour == 10 && state.scheduleEndHour == 23,
                        onClick = { onIntent(MainIntent.UpdateScheduleTime(10, 0, 23, 0)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TimeSelectorTile(
    label: String,
    timeStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = palette.glassSurface,
        border = BorderStroke(1.dp, palette.glassBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Primary.copy(alpha = if (palette.isDark) 0.15f else 0.1f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, fontSize = 10.sp, color = palette.textMuted)
                Text(
                    text = timeStr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
            }
        }
    }
}

@Composable
fun SchedulePresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appPalette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Primary.copy(alpha = 0.2f) else palette.glassSurface,
        border = BorderStroke(1.dp, if (selected) Primary else palette.glassBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Primary else palette.textSecondary
            )
        }
    }
}

@Composable
fun BackgroundHealthCard(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val palette = MaterialTheme.appPalette
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Engine Health",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = if (state.allPermissionsGranted) "100% HEALTHY" else "ATTENTION NEEDED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.allPermissionsGranted) Success else Warning
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HealthDiagnosticRow(
                title = "SMS Transaction Interceptor",
                subtitle = "Catches bank credit notifications offline",
                active = state.smsPermissionGranted
            )
            Spacer(modifier = Modifier.height(10.dp))
            HealthDiagnosticRow(
                title = "Notification Listener",
                subtitle = "Listens to PhonePe, Paytm & GPay alerts",
                active = state.notificationListenerGranted
            )
            Spacer(modifier = Modifier.height(10.dp))
            HealthDiagnosticRow(
                title = "Unrestricted Battery",
                subtitle = "Keeps voice alive when phone screen turns off",
                active = state.batteryOptimizationIgnored
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onIntent(MainIntent.TestSound("500")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (!state.allPermissionsGranted) {
                    OutlinedButton(
                        onClick = onRequestPermissions,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Warning),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning)
                    ) {
                        Text("Fix Permissions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthDiagnosticRow(
    title: String,
    subtitle: String,
    active: Boolean
) {
    val palette = MaterialTheme.appPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.textSecondary
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (active) Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f)
        ) {
            Text(
                text = if (active) "ACTIVE" else "FIX",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (active) Success else Warning,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

fun formatTimeString(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", h, minute, amPm)
}
