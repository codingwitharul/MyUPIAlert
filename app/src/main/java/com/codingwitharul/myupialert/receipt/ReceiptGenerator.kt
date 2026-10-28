package com.codingwitharul.myupialert.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.codingwitharul.myupialert.model.PaymentTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Quick Share Receipt Generator — MyUPIAlert
 *
 * Generates a premium, branded payment confirmation PNG image
 * that merchants can share via Android share sheet (WhatsApp, etc.)
 * No internet required — 100% offline generation.
 */
object ReceiptGenerator {

    private const val TAG = "ReceiptGenerator"
    private const val RECEIPT_WIDTH = 720
    private const val RECEIPT_HEIGHT = 1120

    /**
     * Generate a receipt image for a payment transaction and return the share intent.
     */
    fun generateAndShare(context: Context, transaction: PaymentTransaction): Intent? {
        return try {
            val bitmap = renderReceipt(context, transaction)
            val file = saveBitmapToCache(context, bitmap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.receipt_provider",
                file
            )
            createShareIntent(uri, transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate receipt: ${e.message}", e)
            null
        }
    }

    /**
     * Render a professional receipt to a Bitmap canvas.
     */
    private fun renderReceipt(context: Context, transaction: PaymentTransaction): Bitmap {
        val bitmap = Bitmap.createBitmap(RECEIPT_WIDTH, RECEIPT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val amount = transaction.amount ?: "0"
        val sender = extractCleanSenderName(transaction.sender ?: "")
        val type = transaction.type ?: "credit"
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
        val refId = transaction.id.take(12).uppercase()
        val isCredit = type.equals("credit", ignoreCase = true)

        // === Background ===
        val bgGradient = LinearGradient(
            0f, 0f, 0f, RECEIPT_HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#0F172A"),
                Color.parseColor("#1E293B"),
                Color.parseColor("#0F172A")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = bgGradient }
        canvas.drawRect(0f, 0f, RECEIPT_WIDTH.toFloat(), RECEIPT_HEIGHT.toFloat(), bgPaint)

        // === Decorative top arc glow ===
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, RECEIPT_WIDTH.toFloat(), 200f,
                if (isCredit) Color.parseColor("#10B981") else Color.parseColor("#F43F5E"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            alpha = 60
        }
        canvas.drawOval(RectF(-100f, -200f, RECEIPT_WIDTH + 100f, 250f), glowPaint)

        // === Status Icon Circle ===
        val centerX = RECEIPT_WIDTH / 2f
        val statusColor = if (isCredit) Color.parseColor("#10B981") else Color.parseColor("#F43F5E")
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor; alpha = 30 }
        canvas.drawCircle(centerX, 140f, 65f, circlePaint)
        circlePaint.alpha = 70
        canvas.drawCircle(centerX, 140f, 48f, circlePaint)
        circlePaint.alpha = 255
        canvas.drawCircle(centerX, 140f, 35f, circlePaint)

        // Checkmark or X
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (isCredit) {
            val checkPath = Path().apply {
                moveTo(centerX - 14f, 140f)
                lineTo(centerX - 3f, 153f)
                lineTo(centerX + 18f, 125f)
            }
            canvas.drawPath(checkPath, checkPaint)
        } else {
            canvas.drawLine(centerX - 12f, 128f, centerX + 12f, 152f, checkPaint)
            canvas.drawLine(centerX + 12f, 128f, centerX - 12f, 152f, checkPaint)
        }

        // === "Payment Received" Title ===
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            if (isCredit) "Payment Received ✓" else "Payment Sent",
            centerX, 220f, titlePaint
        )

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val storeName = prefs.getString("store_name", "") ?: ""

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }

        if (storeName.isNotBlank()) {
            val storePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8") // Sky blue highlight
                textSize = 17f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(storeName, centerX, 248f, storePaint)
            canvas.drawText(dateStr, centerX, 272f, subtitlePaint)
        } else {
            canvas.drawText(dateStr, centerX, 250f, subtitlePaint)
        }

        // === Giant Amount ===
        val currencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("₹", centerX - 90f - (amount.length * 12f), 340f, currencyPaint)

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.02f
        }
        canvas.drawText(formatIndianAmount(amount), centerX, 345f, amountPaint)

        // === Dashed Divider ===
        val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        canvas.drawLine(48f, 400f, RECEIPT_WIDTH - 48f, 400f, dashedPaint)

        // === Detail Rows ===
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 18f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var rowY = 460f
        val rowSpacing = 60f
        val leftX = 56f
        val rightX = RECEIPT_WIDTH - 56f

        // Row: Status
        canvas.drawText("Status", leftX, rowY, labelPaint)
        val statusValuePaint = Paint(valuePaint).apply { color = statusColor }
        canvas.drawText(if (isCredit) "SUCCESS" else "SENT", rightX, rowY, statusValuePaint)

        // Row: From / Via
        rowY += rowSpacing
        canvas.drawText(if (isCredit) "Received Via" else "Sent Via", leftX, rowY, labelPaint)
        canvas.drawText(sender, rightX, rowY, valuePaint)

        // Row: Type
        rowY += rowSpacing
        canvas.drawText("Type", leftX, rowY, labelPaint)
        canvas.drawText("UPI Transfer", rightX, rowY, valuePaint)

        // Row: Transaction ID
        rowY += rowSpacing
        canvas.drawText("Transaction ID", leftX, rowY, labelPaint)
        val refPaint = Paint(valuePaint).apply { textSize = 16f; typeface = Typeface.MONOSPACE }
        canvas.drawText(refId, rightX, rowY, refPaint)

        // Row: Date & Time
        rowY += rowSpacing
        canvas.drawText("Date & Time", leftX, rowY, labelPaint)
        val datePaint = Paint(valuePaint).apply { textSize = 16f }
        canvas.drawText(dateStr, rightX, rowY, datePaint)

        // === Second Dashed Divider ===
        rowY += 40f
        canvas.drawLine(48f, rowY, RECEIPT_WIDTH - 48f, rowY, dashedPaint)

        // === Footer Branding ===
        val footerY = rowY + 90f
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("MyUPIAlert", centerX, footerY, brandPaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Free UPI Payment Voice Alert • 100% Offline", centerX, footerY + 26f, taglinePaint)

        // Tiny disclaimer
        val disclaimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This receipt was generated by MyUPIAlert app.", centerX, footerY + 60f, disclaimerPaint)
        canvas.drawText("For official confirmation, check your bank statement.", centerX, footerY + 78f, disclaimerPaint)

        // === Bottom Decorative Line ===
        val bottomLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                100f, 0f, RECEIPT_WIDTH - 100f, 0f,
                Color.TRANSPARENT, statusColor, Shader.TileMode.MIRROR
            )
            strokeWidth = 3f
        }
        canvas.drawLine(100f, RECEIPT_HEIGHT - 30f, RECEIPT_WIDTH - 100f, RECEIPT_HEIGHT - 30f, bottomLinePaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "receipts")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "payment_receipt_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Clean up old receipts (keep only latest 10)
        cacheDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(10)
            ?.forEach { it.delete() }

        return file
    }

    private fun createShareIntent(uri: Uri, transaction: PaymentTransaction): Intent {
        val amount = transaction.amount ?: "0"
        val dateStr = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
        val sender = extractCleanSenderName(transaction.sender ?: "")

        val shareText = "✅ Payment of ₹$amount received via $sender on $dateStr. — MyUPIAlert"

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun extractCleanSenderName(sender: String): String {
        return when {
            sender.contains("phonepe", ignoreCase = true) -> "PhonePe"
            sender.contains("paytm", ignoreCase = true) -> "Paytm"
            sender.contains("paisa", ignoreCase = true) -> "Google Pay"
            sender.contains("fampay", ignoreCase = true) -> "FamPay"
            sender.contains("bharatpe", ignoreCase = true) -> "BharatPe"
            sender.contains("cred", ignoreCase = true) -> "CRED"
            sender.contains("sbi", ignoreCase = true) -> "SBI"
            sender.contains("icici", ignoreCase = true) -> "ICICI"
            sender.contains("hdfc", ignoreCase = true) -> "HDFC"
            sender.contains("axis", ignoreCase = true) -> "Axis Bank"
            sender.contains("amazon", ignoreCase = true) -> "Amazon Pay"
            sender.contains("bob", ignoreCase = true) -> "Bank of Baroda"
            sender.isNotBlank() -> sender.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            else -> "UPI"
        }
    }

    private fun formatIndianAmount(amountStr: String): String {
        return try {
            val parts = amountStr.split(".")
            val wholePart = parts[0].replace(",", "").toLong()
            val decimalPart = if (parts.size > 1) ".${parts[1]}" else ""

            // Indian numbering: last 3, then groups of 2
            val wholeStr = wholePart.toString()
            val formatted = if (wholeStr.length > 3) {
                val last3 = wholeStr.takeLast(3)
                val remaining = wholeStr.dropLast(3)
                val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
                "$grouped,$last3"
            } else {
                wholeStr
            }
            "$formatted$decimalPart"
        } catch (e: Exception) {
            amountStr
        }
    }
}
