package com.codingwitharul.myupialert.dedup

import android.util.Log
import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.repository.SettingsRepository
import com.codingwitharul.myupialert.repository.TransactionRepository
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

enum class DedupAction {
    /** New legitimate payment: announce aloud, record in database, forward SMS. */
    PROCESS_NEW,

    /** Twin alert (app notification + bank SMS confirmation, or OS notification refresh): silently suppress duplicate audio & DB logging. */
    SUPPRESS_TWIN,

    /** Replay of an older payment with matching UTR (> 3 minutes old): announce anti-fraud duplicate warning. */
    WARN_FRAUD_REPLAY
}

data class DedupDecision(
    val action: DedupAction,
    val existingTransaction: PaymentTransaction? = null,
    val reason: String = ""
)

/**
 * Intelligent cross-channel deduplication engine that unifies Push Notifications and Banking SMS.
 * Eliminates duplicate voice announcements, double-entry dashboard totals, and false duplicate alarms.
 */
class PaymentDeduplicator(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) {

    private data class CachedPaymentAlert(
        val amount: Double,
        val amountNormalized: String,
        val utr: String?,
        val sender: String?,
        val source: String, // "notification" or "sms"
        val bodyHash: Int,
        val timestamp: Long,
        var transactionId: String? = null
    )

    private val recentAlerts = ConcurrentLinkedQueue<CachedPaymentAlert>()

    /**
     * Evaluates incoming payment alert and decides whether to process, silently suppress twin, or warn fraud replay.
     */
    suspend fun evaluate(
        amountStr: String,
        senderName: String?,
        utr: String?,
        source: String, // "notification" or "sms"
        rawBody: String
    ): DedupDecision {
        val now = System.currentTimeMillis()
        val amountVal = normalizeAmountToDouble(amountStr)
        val normalizedAmountStr = String.format(Locale.US, "%.2f", amountVal)
        val cleanBody = rawBody.trim()
        val bodyHash = cleanBody.hashCode()
        val cleanUtr = utr?.trim()?.takeIf { it.length >= 6 }

        // 1. Purge entries older than 10 minutes from cache
        recentAlerts.removeIf { now - it.timestamp > CACHE_PURGE_WINDOW_MS }

        // 2. Check in-memory fast-cache for twin alerts arriving seconds apart
        for (alert in recentAlerts) {
            val elapsed = now - alert.timestamp

            // A. Exact identical message or notification repost within 30 seconds
            if (alert.bodyHash == bodyHash && elapsed <= IDENTICAL_BODY_WINDOW_MS) {
                Log.d(TAG, "Suppressed identical repost from ${alert.source} (elapsed: ${elapsed}ms)")
                return DedupDecision(DedupAction.SUPPRESS_TWIN, reason = "Identical message within 30s")
            }

            // B. Definitive match by UTR within twin window (< 3 minutes)
            if (cleanUtr != null && alert.utr != null) {
                if (cleanUtr.equals(alert.utr, ignoreCase = true) && elapsed <= TWIN_WINDOW_MS) {
                    Log.d(TAG, "Suppressed twin alert with matching UTR $cleanUtr (elapsed: ${elapsed}ms, original: ${alert.source})")
                    return DedupDecision(DedupAction.SUPPRESS_TWIN, reason = "Twin alert with matching UTR $cleanUtr")
                }
            }

            // C. Cross-channel twin match: One is Notification, one is SMS, with matching amount within twin window
            if (alert.source != source && amountVal > 0.0 && abs(amountVal - alert.amount) < 0.01 && elapsed <= TWIN_WINDOW_MS) {
                if (isSenderCompatible(senderName, alert.sender)) {
                    Log.d(TAG, "Suppressed cross-channel twin alert: ₹$normalizedAmountStr from $source (matching ${alert.source} ${elapsed}ms ago)")
                    return DedupDecision(DedupAction.SUPPRESS_TWIN, reason = "Cross-channel twin confirmation")
                }
            }
        }

        // 3. Check Room DB for UTR anti-fraud replay or persistent twin detection
        if (cleanUtr != null && settingsRepository.detectDuplicateTransactions) {
            val existing = transactionRepository.findTransactionByRef(cleanUtr, now - ANTI_FRAUD_LOOKBACK_MS)
            if (existing != null) {
                val dbElapsed = now - existing.timestamp
                if (dbElapsed <= TWIN_WINDOW_MS) {
                    // Twin alert logged in DB recently (< 3 minutes)
                    Log.d(TAG, "Suppressed twin alert found in DB: UTR $cleanUtr within ${dbElapsed}ms")
                    return DedupDecision(DedupAction.SUPPRESS_TWIN, existingTransaction = existing, reason = "Twin alert in DB within 3m")
                } else {
                    // Replay of an OLD transaction (> 3 minutes ago) - Anti-fraud warning!
                    val elapsedMinutes = dbElapsed / 60000L
                    Log.w(TAG, "Fraudulent replay detected! UTR $cleanUtr was recorded ${elapsedMinutes}m ago")
                    return DedupDecision(DedupAction.WARN_FRAUD_REPLAY, existingTransaction = existing, reason = "Old UTR replayed from ${elapsedMinutes}m ago")
                }
            }
        }

        // 4. Check Room DB for cross-channel twin by amount if UTR was omitted
        if (cleanUtr == null && amountVal > 0.0) {
            val recentTx = transactionRepository.findRecentByAmount(amountStr, normalizedAmountStr, now - TWIN_WINDOW_MS)
            if (recentTx != null) {
                val dbElapsed = now - recentTx.timestamp
                val isOppositeSource = (source == "sms" && recentTx.sender?.startsWith("com.") == true) ||
                                       (source == "notification" && recentTx.sender?.startsWith("com.") != true)
                if (isOppositeSource && dbElapsed <= 90_000L) {
                    Log.d(TAG, "Suppressed cross-channel twin by amount from DB within ${dbElapsed}ms")
                    return DedupDecision(DedupAction.SUPPRESS_TWIN, existingTransaction = recentTx, reason = "Cross-channel twin in DB within 90s")
                }
            }
        }

        // Legitimate new transaction to process
        return DedupDecision(DedupAction.PROCESS_NEW)
    }

    /**
     * Cache newly accepted transactions to deduplicate future incoming twin alerts.
     */
    fun recordProcessed(
        amountStr: String,
        senderName: String?,
        utr: String?,
        source: String,
        rawBody: String,
        transactionId: String? = null
    ) {
        val amountVal = normalizeAmountToDouble(amountStr)
        val entry = CachedPaymentAlert(
            amount = amountVal,
            amountNormalized = String.format(Locale.US, "%.2f", amountVal),
            utr = utr?.trim()?.takeIf { it.length >= 6 },
            sender = senderName?.trim()?.takeIf { it.isNotBlank() },
            source = source,
            bodyHash = rawBody.trim().hashCode(),
            timestamp = System.currentTimeMillis(),
            transactionId = transactionId
        )
        recentAlerts.add(entry)
    }

    /**
     * Enriches an existing transaction in Room with newly discovered UTR or banking ref if missing.
     */
    suspend fun enrichExistingTransactionIfPossible(existing: PaymentTransaction, newUtr: String?, source: String) {
        if (newUtr.isNullOrBlank()) return
        val currentBody = existing.body.orEmpty()
        if (!currentBody.contains(newUtr, ignoreCase = true)) {
            val updatedBody = "$currentBody (Bank Ref: $newUtr via $source)"
            val updated = existing.copy(body = updatedBody)
            try {
                transactionRepository.update(updated)
                Log.d(TAG, "Enriched existing transaction ${existing.id} with UTR $newUtr")
            } catch (ignored: Exception) {}
        }
    }

    private fun isSenderCompatible(s1: String?, s2: String?): Boolean {
        if (s1.isNullOrBlank() || s2.isNullOrBlank()) return true
        val n1 = s1.lowercase().trim()
        val n2 = s2.lowercase().trim()
        if (n1 == n2) return true
        if (n1.contains(n2) || n2.contains(n1)) return true
        val genericWords = listOf("customer", "upi", "bank", "credit", "account", "a/c", "merchant", "user")
        if (genericWords.any { n1.contains(it) || n2.contains(it) }) return true
        return false
    }

    companion object {
        private const val TAG = "PaymentDeduplicator"

        /** Maximum expected delay between twin notification and SMS delivery (3 minutes). */
        const val TWIN_WINDOW_MS = 180_000L

        /** Maximum window for duplicate OS notification refreshes or broadcast retries (30 seconds). */
        const val IDENTICAL_BODY_WINDOW_MS = 30_000L

        /** Anti-fraud replay lookback window (24 hours). */
        const val ANTI_FRAUD_LOOKBACK_MS = 86_400_000L

        /** In-memory cache purge window (10 minutes). */
        const val CACHE_PURGE_WINDOW_MS = 600_000L

        fun normalizeAmountToDouble(amountStr: String?): Double {
            if (amountStr.isNullOrBlank()) return 0.0
            return try {
                amountStr.replace(",", "").toDouble()
            } catch (e: Exception) {
                0.0
            }
        }
    }
}
