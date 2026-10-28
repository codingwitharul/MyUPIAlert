package com.codingwitharul.myupialert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import android.widget.RemoteViews
import com.codingwitharul.myupialert.MainActivity
import com.codingwitharul.myupialert.R
import com.codingwitharul.myupialert.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Peak Hour Heatmap Widget — MyUPIAlert
 *
 * Displays a compact heatmap of today's busiest payment hours
 * directly on the Android home screen. Updates every 30 minutes.
 *
 * Features:
 * - 24-hour heatmap bar chart with color intensity (cool→hot)
 * - Today's total collection at a glance
 * - Tap to open the full MyUPIAlert dashboard
 */
class HeatmapWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "HeatmapWidget"
        const val ACTION_REFRESH = "com.codingwitharul.myupialert.WIDGET_REFRESH"

        /**
         * Force-refresh all widget instances from anywhere in the app.
         */
        fun refreshAll(context: Context) {
            val intent = Intent(context, HeatmapWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, HeatmapWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, HeatmapWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, ids)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val data = loadTodayHourlyData(context)
                val bitmap = renderHeatmapBitmap(data)
                val views = buildRemoteViews(context, data, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Widget update failed: ${e.message}", e)
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        data: HeatmapData,
        heatmapBitmap: Bitmap
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_heatmap)

        // Set today's total
        val totalFormatted = formatIndianCurrency(data.totalAmount)
        views.setTextViewText(R.id.widget_total_amount, "₹$totalFormatted")
        views.setTextViewText(R.id.widget_total_count, "${data.totalCount} payments")

        // Set peak hour info
        val peakHourStr = if (data.peakHour >= 0) {
            val startHr = data.peakHour
            val endHr = (startHr + 1) % 24
            "${formatHour(startHr)}–${formatHour(endHr)}"
        } else "—"
        views.setTextViewText(R.id.widget_peak_hour, "Peak: $peakHourStr")

        // Set the heatmap bitmap
        views.setImageViewBitmap(R.id.widget_heatmap_image, heatmapBitmap)

        // Click to open app
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Refresh button
        val refreshIntent = Intent(context, HeatmapWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPending = PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

        return views
    }

    private suspend fun loadTodayHourlyData(context: Context): HeatmapData {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val dao = db.transactionDao()

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis

            val transactions = dao.getTransactionsByDateSync(startOfDay, endOfDay)

            val hourlyCounts = IntArray(24)
            val hourlyAmounts = DoubleArray(24)
            var totalAmount = 0.0
            var totalCount = 0

            for (txn in transactions) {
                if (txn.type != "credit") continue
                val txnCal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                val hour = txnCal.get(Calendar.HOUR_OF_DAY)
                hourlyCounts[hour]++
                val amount = txn.amount?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                hourlyAmounts[hour] += amount
                totalAmount += amount
                totalCount++
            }

            val peakHour = hourlyCounts.indices.maxByOrNull { hourlyCounts[it] } ?: -1
            val actualPeak = if (hourlyCounts.getOrNull(peakHour) ?: 0 > 0) peakHour else -1

            HeatmapData(
                hourlyCounts = hourlyCounts,
                hourlyAmounts = hourlyAmounts,
                totalAmount = totalAmount,
                totalCount = totalCount,
                peakHour = actualPeak
            )
        }
    }

    private fun renderHeatmapBitmap(data: HeatmapData): Bitmap {
        val width = 680
        val height = 140
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val maxCount = data.hourlyCounts.max().coerceAtLeast(1)
        val barWidth = (width - 48f) / 24f
        val barGap = 2f
        val maxBarHeight = height - 40f
        val baseY = height - 20f

        // Hour labels
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        for (hour in 0 until 24) {
            val x = 24f + hour * barWidth + barWidth / 2f
            val count = data.hourlyCounts[hour]
            val barH = if (count > 0) {
                (count.toFloat() / maxCount * maxBarHeight).coerceAtLeast(6f)
            } else {
                3f // minimal tick for empty hours
            }

            // Color intensity based on relative count
            val intensity = if (maxCount > 0) count.toFloat() / maxCount else 0f
            val barColor = interpolateColor(intensity)

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = barColor
            }

            val left = x - (barWidth - barGap) / 2f
            val top = baseY - barH
            val right = x + (barWidth - barGap) / 2f
            val bottom = baseY

            val cornerRadius = 3f
            canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, barPaint)

            // Draw hour label for key hours
            if (hour % 4 == 0) {
                canvas.drawText(formatHourShort(hour), x, height.toFloat() - 2f, labelPaint)
            }
        }

        // "Now" indicator arrow
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val nowX = 24f + currentHour * barWidth + barWidth / 2f
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
            style = Paint.Style.FILL
        }
        val arrowTop = 2f
        val arrowPath = android.graphics.Path().apply {
            moveTo(nowX - 5f, arrowTop)
            lineTo(nowX + 5f, arrowTop)
            lineTo(nowX, arrowTop + 8f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        return bitmap
    }

    private fun interpolateColor(intensity: Float): Int {
        // Cool (dark slate) → Warm (emerald) → Hot (amber/orange)
        return when {
            intensity <= 0f -> Color.parseColor("#1E293B")
            intensity < 0.3f -> lerpColor(Color.parseColor("#1E3A5F"), Color.parseColor("#10B981"), intensity / 0.3f)
            intensity < 0.7f -> lerpColor(Color.parseColor("#10B981"), Color.parseColor("#F59E0B"), (intensity - 0.3f) / 0.4f)
            else -> lerpColor(Color.parseColor("#F59E0B"), Color.parseColor("#EF4444"), (intensity - 0.7f) / 0.3f)
        }
    }

    private fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = ((Color.red(from) * (1 - f)) + (Color.red(to) * f)).toInt()
        val g = ((Color.green(from) * (1 - f)) + (Color.green(to) * f)).toInt()
        val b = ((Color.blue(from) * (1 - f)) + (Color.blue(to) * f)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }

    private fun formatHourShort(hour: Int): String {
        return when {
            hour == 0 -> "12a"
            hour < 12 -> "${hour}a"
            hour == 12 -> "12p"
            else -> "${hour - 12}p"
        }
    }

    private fun formatIndianCurrency(amount: Double): String {
        return try {
            val fmt = NumberFormat.getInstance(Locale("en", "IN"))
            fmt.maximumFractionDigits = 0
            fmt.format(amount)
        } catch (e: Exception) {
            amount.toInt().toString()
        }
    }

    data class HeatmapData(
        val hourlyCounts: IntArray,
        val hourlyAmounts: DoubleArray,
        val totalAmount: Double,
        val totalCount: Int,
        val peakHour: Int
    )
}
