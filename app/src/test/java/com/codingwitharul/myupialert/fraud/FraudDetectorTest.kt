package com.codingwitharul.myupialert.fraud

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FraudDetectorTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var detector: FraudDetector

    @Before
    fun setup() {
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        detector = FraudDetector(prefs)
    }

    @Test
    fun testCleanPaymentReturnsClean() {
        val verdict = detector.analyze("500.00", "Google Pay")
        assertEquals(FraudType.CLEAN, verdict.type)
        assertEquals(FraudSeverity.NONE, verdict.severity)
    }

    @Test
    fun testRapidReplayFlaggedOnThirdIdenticalAmount() {
        detector.analyze("250.00", "PhonePe")
        detector.analyze("250.00", "PhonePe")
        val thirdVerdict = detector.analyze("250.00", "PhonePe")

        assertEquals(FraudType.RAPID_REPLAY, thirdVerdict.type)
        assertEquals(FraudSeverity.HIGH, thirdVerdict.severity)
        assertTrue(thirdVerdict.description.contains("250.00"))
    }

    @Test
    fun testBurstFloodFlaggedOnFifthRapidTransaction() {
        detector.analyze("100.00", "GPay")
        detector.analyze("200.00", "PhonePe")
        detector.analyze("300.00", "Paytm")
        detector.analyze("400.00", "CRED")
        val fifthVerdict = detector.analyze("500.00", "BHIM")

        assertEquals(FraudType.BURST_FLOOD, fifthVerdict.type)
        assertEquals(FraudSeverity.MEDIUM, fifthVerdict.severity)
    }

    @Test
    fun testUnusualAmountFlaggedWhenExceedingHistoricalMax() {
        every { prefs.getInt("fraud_total_count", 0) } returns 15
        every { prefs.getFloat("fraud_max_seen_amount", 0f) } returns 2000f

        // 60,000 is > 50,000 min and > 5x 2,000 (10,000)
        val verdict = detector.analyze("60000.00", "HDFC Bank")

        assertEquals(FraudType.UNUSUAL_AMOUNT, verdict.type)
        assertEquals(FraudSeverity.MEDIUM, verdict.severity)
    }

    @Test
    fun testFraudWarningPhraseUsesPipeDelimitersForTts() {
        val verdict = FraudVerdict(
            type = FraudType.RAPID_REPLAY,
            severity = FraudSeverity.HIGH,
            description = "Test description",
            suggestedAction = "Check"
        )
        val phrase = detector.buildFraudWarningPhrase(verdict, "500")

        // Invariant Rule 3: Segmented TTS strings must use pipe delimiters around amount
        assertTrue("TTS phrase must contain pipe delimiter around amount", phrase.contains("|$500|") || phrase.contains("|500|"))
    }
}
