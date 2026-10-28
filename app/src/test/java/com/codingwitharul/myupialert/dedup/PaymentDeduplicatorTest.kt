package com.codingwitharul.myupialert.dedup

import com.codingwitharul.myupialert.model.PaymentTransaction
import com.codingwitharul.myupialert.repository.SettingsRepository
import com.codingwitharul.myupialert.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class PaymentDeduplicatorTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var deduplicator: PaymentDeduplicator

    @Before
    fun setup() {
        transactionRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.detectDuplicateTransactions } returns true
        coEvery { transactionRepository.findTransactionByRef(any(), any()) } returns null
        coEvery { transactionRepository.findRecentByAmount(any(), any(), any()) } returns null
        deduplicator = PaymentDeduplicator(transactionRepository, settingsRepository)
    }

    @Test
    fun testFirstAlertIsAcceptedAsProcessNew() = runTest {
        val decision = deduplicator.evaluate(
            amountStr = "500",
            senderName = "Rahul",
            utr = "123456789012",
            source = "notification",
            rawBody = "Received Rs 500 from Rahul. UPI Ref 123456789012"
        )

        assertEquals(DedupAction.PROCESS_NEW, decision.action)
    }

    @Test
    fun testTwinSmsWithMatchingUtrIsSilentlySuppressed() = runTest {
        // 1. Process notification first
        deduplicator.recordProcessed(
            amountStr = "500",
            senderName = "Rahul",
            utr = "123456789012",
            source = "notification",
            rawBody = "Received Rs 500 from Rahul. UPI Ref 123456789012"
        )

        // 2. Twin Bank SMS arrives 2 seconds later
        val decision = deduplicator.evaluate(
            amountStr = "500.00",
            senderName = "Rahul",
            utr = "123456789012",
            source = "sms",
            rawBody = "A/c credited with Rs 500.00 by UPI Ref 123456789012"
        )

        assertEquals(DedupAction.SUPPRESS_TWIN, decision.action)
    }

    @Test
    fun testTwinSmsWithMatchingAmountAcrossChannelsIsSuppressed() = runTest {
        // 1. Notification arrived (without UTR in preview)
        deduplicator.recordProcessed(
            amountStr = "250",
            senderName = "Amit",
            utr = null,
            source = "notification",
            rawBody = "Payment of ₹250 received from Amit on PhonePe"
        )

        // 2. Bank SMS arrives with same amount
        val decision = deduplicator.evaluate(
            amountStr = "250.00",
            senderName = "Amit",
            utr = "998877665544",
            source = "sms",
            rawBody = "Your A/c has been credited with INR 250.00 on 28-Oct. Ref 998877665544"
        )

        assertEquals(DedupAction.SUPPRESS_TWIN, decision.action)
    }

    @Test
    fun testIdenticalNotificationUpdateWithin30SecondsIsSuppressed() = runTest {
        val body = "Received ₹100 from Suresh via Paytm"
        deduplicator.recordProcessed(
            amountStr = "100",
            senderName = "Suresh",
            utr = null,
            source = "notification",
            rawBody = body
        )

        // Re-posted OS notification
        val decision = deduplicator.evaluate(
            amountStr = "100",
            senderName = "Suresh",
            utr = null,
            source = "notification",
            rawBody = body
        )

        assertEquals(DedupAction.SUPPRESS_TWIN, decision.action)
    }

    @Test
    fun testReplayOfOldUtrFromDatabaseTriggersFraudWarning() = runTest {
        val oldUtr = "430129482918"
        val oldTimestamp = System.currentTimeMillis() - 3600_000L // 1 hour ago

        val existing = PaymentTransaction(
            id = "tx-1",
            amount = "1500",
            type = "credit",
            sender = "com.phonepe.app",
            body = "Payment of Rs 1500 received. Ref $oldUtr",
            timestamp = oldTimestamp
        )

        coEvery { transactionRepository.findTransactionByRef(oldUtr, any()) } returns existing

        val decision = deduplicator.evaluate(
            amountStr = "1500",
            senderName = "Unknown",
            utr = oldUtr,
            source = "sms",
            rawBody = "Fake / replayed SMS with Ref $oldUtr"
        )

        assertEquals(DedupAction.WARN_FRAUD_REPLAY, decision.action)
    }

    @Test
    fun testDistinctTransactionsWithDifferentAmountsAreBothAccepted() = runTest {
        deduplicator.recordProcessed(
            amountStr = "100",
            senderName = "Customer A",
            utr = "111111111111",
            source = "notification",
            rawBody = "Received Rs 100"
        )

        val decision = deduplicator.evaluate(
            amountStr = "200",
            senderName = "Customer B",
            utr = "222222222222",
            source = "notification",
            rawBody = "Received Rs 200"
        )

        assertEquals(DedupAction.PROCESS_NEW, decision.action)
    }
}
