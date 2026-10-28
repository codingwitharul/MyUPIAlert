package com.codingwitharul.myupialert.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*
import java.util.Calendar

/**
 * Dashboard Sticky Status Bar (HUD) — MyUPIAlert
 *
 * Pinned sticky header on the merchant dashboard.
 * Stays visible while scrolling through transactions.
 *
 * Displays:
 * 1. Live Soundbox Engine Status (Live 🟢 / Paused ⏸️ / Muted 🔇)
 * 2. Real-time Today Collection Total & Payment Count
 * 3. Latest Payment Ticker with quick amount preview
 * 4. Fast Quick Actions: Repeat Last Payment, Toggle Mute, Fullscreen Soundbox
 *
 * Can be enabled / disabled from Settings.
 */
@Composable
fun DashboardStickyStatusBar(
    state: MainState,
    onRepeatLast: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSoundbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appPalette

    // Calculate today's stats from state transactions
    val (todayTotal, todayCount, latestPayment) = remember(state.transactions) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        var total = 0.0
        var count = 0
        var latest: PaymentTransaction? = null

        for (t in state.transactions) {
            if (t.type.equals("credit", ignoreCase = true)) {
                if (latest == null || t.timestamp > latest.timestamp) {
                    latest = t
                }
                if (t.timestamp >= startOfDay) {
                    count++
                    val amt = t.amount?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    total += amt
                }
            }
        }
        Triple(total, count, latest)
    }

    // Pulsing animation for the active status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isLive = state.isServiceActiveNow
    val isMuted = state.isMuted
    val statusColor = when {
        !isLive -> Warning
        isMuted -> Error
        else -> Success
    }

    val statusText = when {
        !isLive -> "PAUSED"
        isMuted -> "MUTED"
        else -> "LIVE"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = palette.surface.copy(alpha = if (palette.isDark) 0.92f else 0.96f),
        border = BorderStroke(
            1.dp,
            if (isLive && !isMuted) palette.glassBorder else statusColor.copy(alpha = 0.5f)
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Live Status Pill & Today Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Indicator Pill
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(if (isLive && !isMuted) pulseScale else 1f)
                                .background(statusColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Live Info Ticker
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${formatCurrency(todayTotal)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($todayCount txns)",
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )
                    }

                    val lastText = if (latestPayment != null) {
                        val sender = getCleanSender(latestPayment.sender)
                        val amt = latestPayment.amount ?: "0"
                        "Last: +₹$amt ($sender)"
                    } else {
                        "Ready for payments"
                    }

                    Text(
                        text = lastText,
                        fontSize = 10.sp,
                        color = AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Fast Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Repeat Last Payment Button
                IconButton(
                    onClick = onRepeatLast,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Primary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Repeat Last",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Mute / Unmute Button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isMuted) Error.copy(alpha = 0.15f) else palette.glassSurface,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Error else palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Soundbox Live Display Button
                IconButton(
                    onClick = onOpenSoundbox,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AccentViolet.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Open Soundbox",
                        tint = AccentViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val whole = amount.toLong()
    val numStr = whole.toString()
    if (numStr.length <= 3) return numStr
    val lastThree = numStr.takeLast(3)
    val rest = numStr.dropLast(3)
    val formattedRest = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$formattedRest,$lastThree"
}

private fun getCleanSender(sender: String?): String {
    if (sender.isNullOrBlank()) return "UPI"
    val lower = sender.lowercase()
    return when {
        lower.contains("phonepe") -> "PhonePe"
        lower.contains("paytm") -> "Paytm"
        lower.contains("paisa") || lower.contains("gpay") || lower.contains("google pay") -> "GPay"
        lower.contains("bhim") -> "BHIM"
        lower.contains("cred") -> "CRED"
        lower.contains("bharatpe") -> "BharatPe"
        lower.contains("fampay") -> "FamPay"
        lower.contains("hdfc") -> "HDFC"
        lower.contains("sbi") -> "SBI"
        lower.contains("icici") -> "ICICI"
        else -> sender.substringAfterLast('.').take(10)
    }
}
