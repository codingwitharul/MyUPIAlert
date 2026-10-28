package com.codingwitharul.myupialert.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState
import com.codingwitharul.myupialert.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun AnalyticsScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit
) {
    val palette = MaterialTheme.appPalette
    val scrollState = rememberScrollState()

    val creditTransactions = state.transactions.filter { it.type == "credit" }
    val totalCredits = creditTransactions.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
    val totalDebits = state.transactions.filter { it.type == "debit" }.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
    val totalCount = creditTransactions.size
    val averageTicket = if (totalCount > 0) totalCredits / totalCount else 0.0
    val maxPayment = creditTransactions.maxOfOrNull { it.amount?.toDoubleOrNull() ?: 0.0 } ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Row for Analytics
        FilterSection(
            state = state,
            onIntent = onIntent
        )

        // Executive Overview Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsStatTile(
                title = "Total Volume",
                value = String.format(Locale.US, "₹%,.2f", totalCredits),
                subtitle = "$totalCount payments",
                icon = Icons.Default.TrendingUp,
                color = Success,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatTile(
                title = "Average Ticket",
                value = String.format(Locale.US, "₹%,.1f", averageTicket),
                subtitle = "Per customer",
                icon = Icons.Default.Payments,
                color = Primary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsStatTile(
                title = "Max Single Sale",
                value = String.format(Locale.US, "₹%,.0f", maxPayment),
                subtitle = "Peak transaction",
                icon = Icons.Default.Star,
                color = Warning,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatTile(
                title = "Net Flow",
                value = String.format(Locale.US, "₹%,.0f", totalCredits - totalDebits),
                subtitle = "Inflow vs Outflow",
                icon = Icons.Default.AccountBalance,
                color = AccentCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Primary Hourly Rush-Hour Heatmap / Sales Chart
        HourlySalesAnalyticsCard(transactions = state.transactions)

        // UPI App Market Share Breakdown
        AppShareBreakdownCard(transactions = creditTransactions)

        // Time-of-Day Traffic Distribution
        TimeOfDayDistributionCard(transactions = creditTransactions)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AnalyticsStatTile(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appPalette
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = if (palette.isDark) 0.15f else 0.1f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = palette.textMuted
            )
        }
    }
}

@Composable
fun AppShareBreakdownCard(transactions: List<PaymentTransaction>) {
    val palette = MaterialTheme.appPalette
    val total = transactions.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }

    val appGroups = transactions.groupBy { transaction ->
        val sender = transaction.sender ?: ""
        val lower = sender.lowercase(Locale.ROOT)
        when {
            sender.contains("paisa") || lower.contains("gpay") || lower.contains("google pay") -> "Google Pay"
            sender.contains("phonepe") || lower.contains("phonepe") -> "PhonePe"
            sender.contains("paytm") || lower.contains("paytm") -> "Paytm"
            sender.contains("fampay") || lower.contains("fampay") -> "FamPay"
            sender.contains("cred") || lower.contains("cred") -> "CRED"
            sender.contains("amazon") || lower.contains("amazon") -> "Amazon Pay"
            else -> "Bank SMS / Other"
        }
    }.mapValues { entry ->
        val sum = entry.value.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
        val count = entry.value.size
        val pct = if (total > 0) (sum / total * 100).toFloat() else 0f
        Triple(sum, count, pct)
    }.toList().sortedByDescending { it.second.first }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Volume Share",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "${appGroups.size} Channels",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (appGroups.isEmpty()) {
                Text(
                    text = "No payment channels detected in current filter range.",
                    fontSize = 12.sp,
                    color = palette.textMuted
                )
            } else {
                appGroups.forEach { (appName, data) ->
                    val (sum, count, pct) = data
                    val color = when (appName) {
                        "Google Pay" -> Color(0xFF4285F4)
                        "PhonePe" -> Color(0xFF5F259F)
                        "Paytm" -> Color(0xFF00BAF2)
                        "FamPay" -> Color(0xFFFF9E00)
                        "CRED" -> Color(0xFFE2E8F0)
                        "Amazon Pay" -> Color(0xFFFF9900)
                        else -> Primary
                    }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$appName ($count txns)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.0f (%.1f%%)", sum, pct),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = palette.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeOfDayDistributionCard(transactions: List<PaymentTransaction>) {
    val palette = MaterialTheme.appPalette

    var morningSum = 0.0
    var afternoonSum = 0.0
    var eveningSum = 0.0
    var nightSum = 0.0

    val cal = Calendar.getInstance()
    transactions.forEach { tx ->
        val amt = tx.amount?.toDoubleOrNull() ?: 0.0
        cal.timeInMillis = tx.timestamp
        when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> morningSum += amt
            in 12..16 -> afternoonSum += amt
            in 17..21 -> eveningSum += amt
            else -> nightSum += amt
        }
    }

    val total = morningSum + afternoonSum + eveningSum + nightSum

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sales by Time of Day",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeSlotTile(
                    label = "Morning",
                    hours = "6 AM - 12 PM",
                    amount = morningSum,
                    total = total,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                TimeSlotTile(
                    label = "Afternoon",
                    hours = "12 PM - 5 PM",
                    amount = afternoonSum,
                    total = total,
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeSlotTile(
                    label = "Evening",
                    hours = "5 PM - 10 PM",
                    amount = eveningSum,
                    total = total,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                TimeSlotTile(
                    label = "Night",
                    hours = "10 PM - 6 AM",
                    amount = nightSum,
                    total = total,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TimeSlotTile(
    label: String,
    hours: String,
    amount: Double,
    total: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appPalette
    val pct = if (total > 0) (amount / total * 100).toInt() else 0

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = palette.glassSurface,
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = "$pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textMuted)
            }
            Text(text = hours, fontSize = 9.sp, color = palette.textMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "₹%.0f", amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }
    }
}
