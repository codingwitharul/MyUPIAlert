package com.codingwitharul.myupialert.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.ui.theme.*
import java.util.Calendar

/**
 * 100% Offline Hourly Rush-Hour Heatmap and Sales Analytics Card.
 * Groups payments into 24 hour buckets to visualize peak morning, lunch, and evening rush hours.
 */
@Composable
fun HourlySalesAnalyticsCard(transactions: List<PaymentTransaction>) {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis

    val todayCredits = transactions.filter { it.timestamp >= startOfDay && it.type == "credit" }

    // Aggregate by 3-hour blocks for clean mobile display:
    // 00-03, 03-06, 06-09, 09-12, 12-15, 15-18, 18-21, 21-24
    val blockLabels = listOf("6AM", "9AM", "12PM", "3PM", "6PM", "9PM", "12AM")
    val hourlyTotals = DoubleArray(8) { 0.0 }
    val hourlyCounts = IntArray(8) { 0 }

    for (t in todayCredits) {
        val txnCal = Calendar.getInstance().apply { timeInMillis = t.timestamp }
        val hour = txnCal.get(Calendar.HOUR_OF_DAY)
        val bucket = (hour / 3).coerceIn(0, 7)
        val amount = t.amount?.toDoubleOrNull() ?: 0.0
        hourlyTotals[bucket] += amount
        hourlyCounts[bucket]++
    }

    val maxTotal = hourlyTotals.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
    val peakBucketIndex = hourlyTotals.indices.maxByOrNull { hourlyTotals[it] } ?: 0
    val peakTimeLabel = when (peakBucketIndex) {
        2 -> "6 AM – 9 AM (Morning)"
        3 -> "9 AM – 12 PM (Late Morning)"
        4 -> "12 PM – 3 PM (Lunch Rush)"
        5 -> "3 PM – 6 PM (Afternoon)"
        6 -> "6 PM – 9 PM (Evening Peak)"
        7 -> "9 PM – 12 AM (Night)"
        else -> "Early Hours"
    }

    val palette = MaterialTheme.appPalette

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.glassCard),
        border = BorderStroke(1.dp, palette.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📈", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today's Rush-Hour Activity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                Text(
                    text = "Peak: $peakTimeLabel",
                    fontSize = 11.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bar Chart Visualization
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val chartBuckets = (1..7) // 3 AM to midnight
                chartBuckets.forEach { b ->
                    val bucketAmt = hourlyTotals[b]
                    val heightFraction = (bucketAmt / maxTotal).toFloat().coerceIn(0.08f, 1.0f)
                    val isPeak = b == peakBucketIndex && bucketAmt > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(heightFraction)
                                .background(
                                    if (isPeak) {
                                        Brush.verticalGradient(listOf(Success, Color(0xFF059669)))
                                    } else {
                                        Brush.verticalGradient(listOf(Primary.copy(alpha = 0.8f), PrimaryDark.copy(alpha = 0.4f)))
                                    },
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = blockLabels.getOrElse(b - 1) { "" },
                            fontSize = 9.sp,
                            color = if (isPeak) Success else palette.textSecondary,
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
