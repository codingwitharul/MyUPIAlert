package com.codingwitharul.myupialert.fraud

import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Smart Fraud Detection Engine — MyUPIAlert
 *
 * Detects suspicious payment patterns locally (100% offline):
 * 1. RAPID_REPLAY: Same exact amount received 3+ times within 60 seconds
 * 2. UNUSUAL_AMOUNT: Amount exceeds merchant's typical maximum by 5x
 * 3. BURST_FLOOD: 5+ different payments within 30 seconds (transaction flooding)
 *
 * All thresholds are adaptive and calibrate to the merchant's actual transaction history.
 */
class FraudDetector(private val prefs: SharedPreferences) {

    companion object {
        private const val TAG = "FraudDetector"
        private const val PREF_MAX_SEEN_AMOUNT = "fraud_max_seen_amount"
        private const val PREF_AVG_AMOUNT = "fraud_avg_amount"
        private const val PREF_TOTAL_COUNT = "fraud_total_count"

        // Thresholds
        private const val RAPID_REPLAY_WINDOW_MS = 60_000L       // 60 seconds
        private const val RAPID_REPLAY_COUNT = 3                  // 3 same-amounts in window
        private const val BURST_FLOOD_WINDOW_MS = 30_000L         // 30 seconds
        private const val BURST_FLOOD_COUNT = 5                   // 5 transactions in window
        private const val UNUSUAL_AMOUNT_MULTIPLIER = 5.0         // 5x the historical max
        private const val MIN_HISTORY_BEFORE_UNUSUAL = 10         // Need 10+ txns before flagging
        private const val UNUSUAL_ABSOLUTE_MIN = 50_000.0         // Don't flag below ₹50,000
    }

    // In-memory sliding window of recent payments (amount, timestamp)
    private data class RecentPayment(val amount: Double, val timestamp: Long)
    private val recentPayments = ConcurrentLinkedDeque<RecentPayment>()

    /**
     * Analyze an incoming payment and return a [FraudVerdict].
     * Call this BEFORE speaking the TTS announcement.
     */
    fun analyze(amountStr: String, senderName: String): FraudVerdict {
        val amount = parseAmount(amountStr)
        val now = System.currentTimeMillis()

        // Cleanup stale entries older than 2 minutes
        recentPayments.removeIf { now - it.timestamp > 120_000L }

        // Add current payment to sliding window
        recentPayments.addLast(RecentPayment(amount, now))

        // --- Check 1: Rapid Replay (same amount repeated rapidly) ---
        val sameAmountRecent = recentPayments.count {
            it.amount == amount && (now - it.timestamp) <= RAPID_REPLAY_WINDOW_MS
        }
        if (sameAmountRecent >= RAPID_REPLAY_COUNT) {
            Log.w(TAG, "RAPID_REPLAY detected: ₹$amountStr received $sameAmountRecent times in ${RAPID_REPLAY_WINDOW_MS / 1000}s")
            return FraudVerdict(
                type = FraudType.RAPID_REPLAY,
                severity = FraudSeverity.HIGH,
                description = "Same amount ₹$amountStr received $sameAmountRecent times within 1 minute",
                suggestedAction = "Verify with customer before accepting. Possible replay attack."
            )
        }

        // --- Check 2: Burst Flood (too many different payments too fast) ---
        val burstCount = recentPayments.count {
            (now - it.timestamp) <= BURST_FLOOD_WINDOW_MS
        }
        if (burstCount >= BURST_FLOOD_COUNT) {
            Log.w(TAG, "BURST_FLOOD detected: $burstCount payments in ${BURST_FLOOD_WINDOW_MS / 1000}s")
            return FraudVerdict(
                type = FraudType.BURST_FLOOD,
                severity = FraudSeverity.MEDIUM,
                description = "$burstCount payments received within 30 seconds",
                suggestedAction = "Unusual transaction speed. Verify all recent payments."
            )
        }

        // --- Check 3: Unusual Amount (way beyond merchant's typical range) ---
        val totalCount = prefs.getInt(PREF_TOTAL_COUNT, 0)
        if (totalCount >= MIN_HISTORY_BEFORE_UNUSUAL && amount >= UNUSUAL_ABSOLUTE_MIN) {
            val maxSeen = prefs.getFloat(PREF_MAX_SEEN_AMOUNT, 0f).toDouble()
            if (maxSeen > 0 && amount > maxSeen * UNUSUAL_AMOUNT_MULTIPLIER) {
                Log.w(TAG, "UNUSUAL_AMOUNT detected: ₹$amountStr (max seen: ₹$maxSeen)")
                return FraudVerdict(
                    type = FraudType.UNUSUAL_AMOUNT,
                    severity = FraudSeverity.MEDIUM,
                    description = "₹$amountStr is ${(amount / maxSeen).toInt()}x your usual maximum (₹${maxSeen.toInt()})",
                    suggestedAction = "Unusually large payment. Confirm with customer."
                )
            }
        }

        // --- Update running statistics ---
        updateStats(amount)

        return FraudVerdict(
            type = FraudType.CLEAN,
            severity = FraudSeverity.NONE,
            description = "No suspicious patterns detected",
            suggestedAction = null
        )
    }

    /**
     * Build a TTS warning phrase for fraud alerts.
     * Uses pipe delimiters around amounts per project convention.
     */
    fun buildFraudWarningPhrase(verdict: FraudVerdict, amountStr: String): String {
        return when (verdict.type) {
            FraudType.RAPID_REPLAY ->
                "Warning! Suspicious repeat payment detected. Amount |$amountStr| rupees received multiple times rapidly. Please verify with customer."
            FraudType.BURST_FLOOD ->
                "Warning! Too many payments received in a short time. Latest amount |$amountStr| rupees. Please check all recent transactions."
            FraudType.UNUSUAL_AMOUNT ->
                "Attention! Unusually large payment of |$amountStr| rupees received. This is much higher than your normal transactions. Please confirm with customer."
            FraudType.CLEAN -> "" // Should not be called for clean
        }
    }

    private fun updateStats(amount: Double) {
        val totalCount = prefs.getInt(PREF_TOTAL_COUNT, 0)
        val currentMax = prefs.getFloat(PREF_MAX_SEEN_AMOUNT, 0f).toDouble()
        val currentAvg = prefs.getFloat(PREF_AVG_AMOUNT, 0f).toDouble()

        val newCount = totalCount + 1
        val newMax = maxOf(currentMax, amount)
        // Running average
        val newAvg = if (totalCount == 0) amount else (currentAvg * totalCount + amount) / newCount

        prefs.edit()
            .putInt(PREF_TOTAL_COUNT, newCount)
            .putFloat(PREF_MAX_SEEN_AMOUNT, newMax.toFloat())
            .putFloat(PREF_AVG_AMOUNT, newAvg.toFloat())
            .apply()
    }

    private fun parseAmount(amountStr: String): Double {
        return try {
            amountStr.replace(",", "").replace(" ", "").toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    /** Get fraud statistics for display in UI */
    fun getStats(): FraudStats {
        return FraudStats(
            totalAnalyzed = prefs.getInt(PREF_TOTAL_COUNT, 0),
            maxSeenAmount = prefs.getFloat(PREF_MAX_SEEN_AMOUNT, 0f).toDouble(),
            avgAmount = prefs.getFloat(PREF_AVG_AMOUNT, 0f).toDouble()
        )
    }
}

data class FraudVerdict(
    val type: FraudType,
    val severity: FraudSeverity,
    val description: String,
    val suggestedAction: String?
)

enum class FraudType {
    CLEAN,
    RAPID_REPLAY,
    BURST_FLOOD,
    UNUSUAL_AMOUNT
}

enum class FraudSeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

data class FraudStats(
    val totalAnalyzed: Int,
    val maxSeenAmount: Double,
    val avgAmount: Double
)
