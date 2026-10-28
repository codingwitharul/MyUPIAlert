package com.codingwitharul.myupialert.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Merchant Counter Mode: High-contrast ambient Soundbox display designed for shop counters.
 * Displays large readable collection totals, live digital clock, animated waveform rings,
 * and high-visibility transaction ticker readable from 5+ feet away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundboxScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onExitSoundbox: () -> Unit
) {
    val context = LocalContext.current

    // Keep screen awake while in counter mode if enabled in settings
    DisposableEffect(state.soundboxKeepScreenOn) {
        val window = (context as? Activity)?.window
        if (state.soundboxKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Live clock ticker
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    // Filter today's transactions
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    val todayTransactions = state.transactions.filter { it.timestamp >= startOfDay }
    val todayCredits = todayTransactions.filter { it.type == "credit" }
    val todayTotal = todayCredits.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
    val recentPayments = state.transactions.take(5)

    // Breathing pulse animation for live soundbox effect
    val infiniteTransition = rememberInfiniteTransition(label = "soundboxPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        containerColor = Color(0xFF040711)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF030712),
                            Color(0xFF0B132B),
                            Color(0xFF030712)
                        )
                    )
                )
        ) {
            // Ambient glow orbs
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Success.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Exit button, Store Name, and Live Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExitSoundbox,
                        modifier = Modifier
                            .size(42.dp)
                            .background(GlassSurface, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Soundbox Mode",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.storeName.isNotBlank()) state.storeName else "UPI SMART SOUNDBOX",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Success, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE & LISTENING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Success,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Speak Summary Button
                    IconButton(
                        onClick = { onIntent(MainIntent.SpeakTodaySummary) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(Primary, AccentViolet)),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Today's Summary",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Digital Clock
                val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)
                Text(
                    text = timeFormat.format(currentTime),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = AccentCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = dateFormat.format(currentTime),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Massive Today's Collection Card with Pulse Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Outer pulse ring
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Success.copy(alpha = 0.08f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassCard),
                        border = BorderStroke(1.5.dp, Success.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "TODAY'S TOTAL COLLECTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", todayTotal),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Success,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x2E10B981), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${todayCredits.size} Received Today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Live Recent Payments Counter Ticker Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE COUNTER TICKER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Last 5 Transactions",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // High-Visibility Transaction Ticker List
                if (recentPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for customer payment...",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(recentPayments, key = { it.id }) { txn ->
                            SoundboxTickerCard(transaction = txn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundboxTickerCard(transaction: PaymentTransaction) {
    val isCredit = transaction.type == "credit"
    val cleanSender = getCleanSenderName(transaction.sender)
    val appGradient = getAppGradient(cleanSender)
    val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(transaction.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardElevated),
        border = BorderStroke(
            1.dp,
            if (isCredit) Success.copy(alpha = 0.35f) else Error.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Brush.linearGradient(appGradient), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getSenderMonogram(cleanSender),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = cleanSender,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = timeStr,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) "+₹${transaction.amount}" else "-₹${transaction.amount}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCredit) Success else Error
                )
                Text(
                    text = if (isCredit) "CREDITED" else "DEBITED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) Success else Error
                )
            }
        }
    }
}
