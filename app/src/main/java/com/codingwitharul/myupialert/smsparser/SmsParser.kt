package com.codingwitharul.myupialert.smsparser

import android.content.Context
import android.content.res.Configuration
import com.codingwitharul.myupialert.R
import java.util.Locale
import java.util.regex.Pattern

class SmsParser {

    fun extractAmount(body: String?): String {
        if (body == null) return "0.00"

        // Indian comma-aware regex: matches 1,00,000 AND 1,000 AND plain 4444
        val indianAmountGroup = "(?:\\d{1,3}(?:,\\d{2,3})+|\\d+)(?:\\.\\d{1,2})?"

        // m0: leading decimal like .50 preceded by currency symbol
        val m0 = Pattern.compile("(?i)(?:[\\u20b9]|rs\\.?|inr|mrp)[\\s]*(\\.\\d{1,2})(?!\\d)").matcher(body)
        if (m0.find()) {
            val valStr = "0" + (m0.group(1) ?: "")
            try {
                if (valStr.toDouble() > 0) return valStr
            } catch (ignored: NumberFormatException) {}
        }

        // m1: currency symbol then amount (₹1,00,000 or Rs 500.00)
        val m1 = Pattern.compile("(?i)(?:[\\u20b9]|rs\\.?|inr|mrp)[\\s]*($indianAmountGroup)").matcher(body)
        if (m1.find()) return m1.group(1)?.replace(",", "") ?: "0.00"

        // m2: amount then currency symbol (1,00,000 Rs)
        val m2 = Pattern.compile("(?i)($indianAmountGroup)\\s*(?:rs\\.?|inr|[\\u20b9])").matcher(body)
        if (m2.find()) return m2.group(1)?.replace(",", "") ?: "0.00"

        // m3: bare numeric fallback (no currency marker)
        val m3 = Pattern.compile("(?<![\\d@,])(\\d{1,7}(?:\\.\\d{1,2})?)(?![\\d])").matcher(body)
        while (m3.find()) {
            val candidate = m3.group(1)
            if (candidate != null) {
                try {
                    val candidateVal = candidate.toDouble()
                    if (candidateVal in 1.0..9999999.0) return candidate
                } catch (ignored: NumberFormatException) {}
            }
        }
        return "0.00"
    }

    fun getSenderName(body: String?): String {
        if (body == null) return ""

        // 1. FamPay #fampaid Native Extraction
        if (body.contains("#fampaid", ignoreCase = true) || body.contains("fampay", ignoreCase = true)) {
            val fp1 = Pattern.compile("(?i)(?:#fampaid[:\\s!]+)?([A-Za-z][a-zA-Z\\s]{1,28}?)\\s+(?:has\\s+paid\\s+you|paid\\s+you)")
            val fm1 = fp1.matcher(body)
            if (fm1.find()) {
                val n = cleanName(fm1.group(1))
                if (isValidName(n)) return n
            }

            val fp2 = Pattern.compile("(?i)(?:from|by)\\s+([A-Za-z][a-zA-Z\\s]{1,28}?)(?=\\s+(?:via|on|using)?\\s+fampay|\\s+#fampaid|$)")
            val fm2 = fp2.matcher(body)
            if (fm2.find()) {
                val n = cleanName(fm2.group(1))
                if (isValidName(n)) return n
            }

            val fp3 = Pattern.compile("(?i)(?:#fampaid[:\\s!]+)([A-Za-z][a-zA-Z\\s]{1,28}?)\\s+sent")
            val fm3 = fp3.matcher(body)
            if (fm3.find()) {
                val n = cleanName(fm3.group(1))
                if (isValidName(n)) return n
            }
        }

        val p1 = Pattern.compile("(?i)(?:paid|sent|transferred)\\s+by\\s+([A-Z][a-zA-Z\\s]{1,28}?)(?=\\s+(?:via|on|at|using|to|ref|upi|\\d)|(?:\\[.]|,|$))")
        val m1 = p1.matcher(body)
        if (m1.find()) {
            val n = cleanName(m1.group(1))
            if (isValidName(n)) return n
        }

        val p2 = Pattern.compile("(?i)from\\s+([A-Z][a-zA-Z\\s]{1,28}?)(?=\\s+(?:via|on|at|using|upi|ref|has|\\d)|(?:\\.|,|\\())")
        val m2 = p2.matcher(body)
        if (m2.find()) {
            val n = cleanName(m2.group(1))
            if (isValidName(n)) return n
        }

        val p3 = Pattern.compile("(?i)([A-Z][a-zA-Z\\s]{1,28}?)\\s+has\\s+(?:paid|sent|transferred|credited)")
        val m3 = p3.matcher(body)
        if (m3.find()) {
            val n = cleanName(m3.group(1))
            if (isValidName(n)) return n
        }

        val p4 = Pattern.compile("(?i)(?:from\\s+VPA|from\\s+UPI\\s+ID|VPA)[:\\s]+([a-zA-Z0-9][a-zA-Z0-9._-]{1,30})@[a-zA-Z]+")
        val m4 = p4.matcher(body)
        if (m4.find()) {
            val g1 = m4.group(1)
            if (g1 != null) {
                val n = extractNameFromVpa(g1)
                if (isValidName(n)) return n
            }
        }

        val p5 = Pattern.compile("([a-zA-Z0-9][a-zA-Z0-9._-]{1,30})@(?:ybl|upi|paytm|okicici|oksbi|okaxis|okhdfcbank|ibl|axl|apl|waicici|hdfcbank|icici|sbi|axis|kotak|barodampay|pnb|indus|rbl|federal|idbi|aubank|yesbank)")
        val m5 = p5.matcher(body)
        if (m5.find()) {
            val g1 = m5.group(1)
            if (g1 != null) {
                val n = extractNameFromVpa(g1)
                if (isValidName(n)) return n
            }
        }

        val p6 = Pattern.compile("(?i)(?:trf\\s+frm|trf\\s+from|imps\\s+frm|imps\\s+from|neft\\s+from|neft\\s+frm)[:\\s]+([A-Z][A-Z\\s]{1,30})")
        val m6 = p6.matcher(body)
        if (m6.find()) {
            val n = cleanName(m6.group(1))
            if (isValidName(n)) return n
        }

        val p7 = Pattern.compile("(?i)received\\s+from\\s+([A-Z][a-zA-Z\\s]{1,28}?)(?=\\s+(?:via|on|at|ref|upi|for)|(?:\\.|,|\\(|$))")
        val m7 = p7.matcher(body)
        if (m7.find()) {
            val n = cleanName(m7.group(1))
            if (isValidName(n)) return n
        }

        val p8 = Pattern.compile("(?i)(?:by|from)[\\s:]+([A-Za-z][a-zA-Z0-9@\\.\\s]+?)(?=\\s+(?:on|ref|upi|txn|at|date|\\d)|(?:[\\.,;\\(]|$))")
        val m8 = p8.matcher(body)
        if (m8.find()) {
            val n = cleanName(m8.group(1))
            if (isValidName(n)) return n
        }
        return ""
    }

    private fun extractNameFromVpa(vpa: String): String {
        val cleaned = vpa.replace("\\d+$".toRegex(), "").replace("[._\\-]".toRegex(), " ").trim().replace("\\d".toRegex(), "").trim()
        return capitalizeName(cleaned)
    }

    private fun cleanName(name: String?): String {
        if (name == null) return ""
        return name.trim().replace("\\s+".toRegex(), " ")
    }

    private fun isValidName(name: String?): Boolean {
        if (name == null || name.isEmpty()) return false
        if (name.length < 2 || name.length > 40) return false
        val lower = name.lowercase(Locale.ROOT)
        val rejects = arrayOf(
            "bank", "upi", "account", "acct", "ref", "neft", "imps", "rtgs", "your", "you",
            "the", "for", "via", "with", "amount", "rupees", "rs", "inr", "credit", "debit",
            "transaction", "transfer", "payment", "service", "limited", "pvt", "ltd", "inc",
            "corp", "dear", "customer", "user", "member", "number", "mobile", "phone", "wallet",
            "paytm", "phonepe", "gpay", "bhim", "google", "amazon"
        )
        for (reject in rejects) {
            if (lower == reject) return false
        }
        if (!name.matches(".*[a-zA-Z].*".toRegex())) return false
        if (name == name.uppercase(Locale.ROOT) && name.length <= 6 && !name.contains(" ")) return false
        return true
    }

    private fun capitalizeName(name: String?): String {
        if (name == null || name.isEmpty()) return ""
        val words = name.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (word in words) {
            if (word.isEmpty()) continue
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(word[0].uppercaseChar())
            if (word.length > 1) sb.append(word.substring(1).lowercase(Locale.ROOT))
        }
        return sb.toString()
    }

    fun getAmountFromMessageBody(context: Context?, body: String?, language: String, announceSender: Boolean, senderName: String?): String {
        if (context == null) {
            return getAmountFromMessageBody(body, language)
        }
        val localizedCtx = getLocalizedContext(context, language)
        if (body.isNullOrEmpty()) return localizedCtx.getString(R.string.tts_unknown_amount)
        val rawAmount = extractAmount(body)
        if (rawAmount == "0.00") return localizedCtx.getString(R.string.tts_unknown_amount)
        val parts = rawAmount.split("\\.".toRegex(), limit = 2).toTypedArray()
        val rupees = parts[0]
        val paisa = if (parts.size > 1) parts[1] else "0"
        var rVal = 0
        var pVal = 0
        try { rVal = rupees.toInt() } catch (ignored: NumberFormatException) {}
        try { pVal = paisa.toInt() } catch (ignored: NumberFormatException) {}
        if (rVal == 0 && pVal == 0) return localizedCtx.getString(R.string.tts_unknown_amount)

        var cleanSender = senderName
        if (!cleanSender.isNullOrEmpty()) {
            cleanSender = cleanSender.replace("[^a-zA-Z\\s\\u0900-\\u097F\\u0B80-\\u0BFF\\u0C00-\\u0C7F\\u0C80-\\u0CFF\\u0D00-\\u0D7F\\u0A80-\\u0AFF\\u0A00-\\u0A7F\\u0980-\\u09FF]".toRegex(), " ").trim()
            cleanSender = if (cleanSender.isEmpty()) null else capitalizeName(cleanSender)
        }
        val hasSender = announceSender && !cleanSender.isNullOrEmpty() && isValidName(cleanSender)

        return if (hasSender) {
            if (pVal != 0) {
                localizedCtx.getString(R.string.tts_rupees_and_paise_with_sender, cleanSender, rupees, paisa)
            } else {
                localizedCtx.getString(R.string.tts_rupees_only_with_sender, cleanSender, rupees)
            }
        } else {
            if (pVal != 0) {
                localizedCtx.getString(R.string.tts_rupees_and_paise_no_sender, rupees, paisa)
            } else {
                localizedCtx.getString(R.string.tts_rupees_only_no_sender, rupees)
            }
        }
    }

    fun getAmountFromMessageBody(body: String?, language: String): String {
        val defaultMsg = AnnouncementPhrases.getUnknownAmountPhrase(language)
        if (body.isNullOrEmpty()) return defaultMsg

        val rawAmount = extractAmount(body)
        if (rawAmount == "0.00") return defaultMsg

        val parts = rawAmount.split("\\.".toRegex(), limit = 2).toTypedArray()
        val rupees = parts[0]
        val paisa = if (parts.size > 1) parts[1] else "0"

        var rVal = 0
        var pVal = 0
        try { rVal = rupees.toInt() } catch (ignored: NumberFormatException) {}
        try { pVal = paisa.toInt() } catch (ignored: NumberFormatException) {}

        if (rVal == 0 && pVal == 0) return defaultMsg

        return AnnouncementPhrases.buildPhrase(language, rupees, if (pVal > 0) paisa else null, null)
    }

    private fun getLocalizedContext(context: Context, language: String): Context {
        val lang = language.lowercase(Locale.ROOT)
        val langCode = when {
            lang.contains("hindi") -> "hi"
            lang.contains("tamil") -> "ta"
            lang.contains("telugu") -> "te"
            lang.contains("kannada") -> "kn"
            lang.contains("malayalam") -> "ml"
            lang.contains("gujarati") -> "gu"
            lang.contains("punjabi") -> "pa"
            lang.contains("bengali") -> "bn"
            lang.contains("marathi") -> "mr"
            lang.contains("odia") -> "or"
            lang.contains("assamese") -> "as"
            lang.contains("urdu") -> "ur"
            else -> "en"
        }

        val locale = Locale(langCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private val forensicBlacklistPatterns = listOf(
        // Loan alerts & EMI
        "\\bloan\\b", "pre-?approved", "apply loan", "loan offer", "instant loan",
        "personal loan", "\\bdisbursed\\b", "\\bemi\\b", "credit limit", "overdraft",
        "\\bsanctioned\\b", "eligible for (?:a )?loan",
        // Coins & Rewards
        "\\bsupercoins?\\b", "\\bcoins?\\b", "reward points?", "cashback points?",
        "scratch card", "spin & win", "\\btokens?\\b", "play & win", "\\bcoupons?\\b",
        // LPG Booking
        "\\blpg\\b", "gas booking", "\\bcylinder\\b", "\\bindane\\b", "hp gas", "bharat gas",
        "gas refill", "cylinder booked",
        // Telco / Utility / OTP / Promo
        "\\bdue\\b", "\\brecharge\\b", "\\bexpiring\\b", "\\bplan\\b", "\\botp\\b",
        "\\bvalidity\\b", "\\bdata pack\\b", "\\bbill due\\b", "\\boutstanding\\b"
    ).map { Pattern.compile("(?i)$it") }

    private fun hasBankingIndicators(lb: String): Boolean {
        return lb.contains("a/c") || lb.contains("acct") || lb.contains("account") ||
                lb.contains("bank") || lb.contains("upi") || lb.contains("vpa") ||
                lb.contains("inr") || lb.contains("rs") || lb.contains("₹") ||
                lb.contains("#fampaid") || lb.contains("fampay")
    }

    private fun hasBlacklistedKeywords(lb: String): Boolean {
        for (pattern in forensicBlacklistPatterns) {
            if (pattern.matcher(lb).find()) return true
        }
        return false
    }

    fun isCreditTransaction(body: String?): Boolean {
        if (body.isNullOrEmpty()) return false
        val lb = body.lowercase(Locale.ROOT)

        if (hasBlacklistedKeywords(lb)) return false
        if (!hasBankingIndicators(lb)) return false
        if (lb.contains("failed") || lb.contains("declined")) return false

        val creditIdx = indexOfAny(
            lb, "credited", "received", "deposited", "deposit", "added", "paid to you",
            "paid you", "has paid", "sent to you", "sent you", "has sent", "transfer from",
            "transferred from", "money received", "payment received", "#fampaid", "प्राप्त", "जमा", "मिले"
        )
        var debitIdx = indexOfAny(
            lb, "debited", "debit", "withdrawn", "withdrawal", "spent", "deducted", "charges",
            "transfer to", "transferred to"
        )

        if (lb.contains("paid") && !lb.contains("paid to you") && !lb.contains("paid you") && !lb.contains("has paid") && !lb.contains("paid by")) {
            val idx = lb.indexOf("paid")
            if (idx != -1 && idx < debitIdx) debitIdx = idx
        }
        if (lb.contains("sent") && !lb.contains("sent to you") && !lb.contains("sent you") && !lb.contains("has sent") && !lb.contains("sent by")) {
            val idx = lb.indexOf("sent")
            if (idx != -1 && idx < debitIdx) debitIdx = idx
        }

        if (creditIdx == Int.MAX_VALUE && debitIdx == Int.MAX_VALUE) return false
        return creditIdx < debitIdx
    }

    fun isDebitTransaction(body: String?): Boolean {
        if (body.isNullOrEmpty()) return false
        val lb = body.lowercase(Locale.ROOT)

        if (hasBlacklistedKeywords(lb)) return false
        if (!hasBankingIndicators(lb)) return false
        if (lb.contains("failed") || lb.contains("declined")) return false

        val creditIdx = indexOfAny(
            lb, "credited", "received", "deposited", "deposit", "added", "paid to you",
            "paid you", "has paid", "sent to you", "sent you", "has sent", "transfer from",
            "transferred from", "money received", "payment received", "प्राप्त", "जमा", "मिले"
        )
        var debitIdx = indexOfAny(
            lb, "debited", "debit", "withdrawn", "withdrawal", "spent", "deducted", "charges",
            "transfer to", "transferred to"
        )

        if (lb.contains("paid") && !lb.contains("paid to you") && !lb.contains("has paid") && !lb.contains("paid by")) {
            val idx = lb.indexOf("paid")
            if (idx != -1 && idx < debitIdx) debitIdx = idx
        }
        if (lb.contains("sent") && !lb.contains("sent to you") && !lb.contains("has sent") && !lb.contains("sent by")) {
            val idx = lb.indexOf("sent")
            if (idx != -1 && idx < debitIdx) debitIdx = idx
        }

        if (debitIdx == Int.MAX_VALUE) return false
        return debitIdx < creditIdx
    }

    private fun indexOfAny(text: String, vararg keywords: String): Int {
        var minIdx = Int.MAX_VALUE
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx != -1 && idx < minIdx) minIdx = idx
        }
        return minIdx
    }

    fun extractReferenceNumber(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val pattern = Pattern.compile("(?i)(?:upi ref|ref no|ref|utr|txn id|rrn)[:\\s-]*([0-9a-zA-Z]{6,})")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
}
