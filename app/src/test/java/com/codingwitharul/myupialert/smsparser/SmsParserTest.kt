package com.codingwitharul.myupialert.smsparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun parsesWholeRupeeAmountsWithoutCrashing() {
        val parser = SmsParser()
        val result = parser.getAmountFromMessageBody("Your payment of Rs 10 was received", "English")
        assertEquals("Received 10 rupees", result)
    }

    @Test
    fun parsesDecimalAmountsWithPaise() {
        val parser = SmsParser()
        val result = parser.getAmountFromMessageBody("Your payment of INR 100.25 was received", "English")
        assertEquals("Received 100 rupees and 25 paisa", result)
    }

    @Test
    fun parsesIndianCommaAmounts() {
        val parser = SmsParser()
        val amount = parser.extractAmount("A/C credited with INR 1,50,000.50 via UPI")
        assertEquals("150000.50", amount)
    }

    @Test
    fun classifiesCreditAndDebitTransactions() {
        val parser = SmsParser()
        assertTrue(parser.isCreditTransaction("INR 500.00 credited to A/C ending 1234 on 24-Oct"))
        assertFalse(parser.isCreditTransaction("INR 500.00 debited from A/C ending 1234"))
        assertTrue(parser.isDebitTransaction("INR 500.00 debited from A/C ending 1234"))
        assertFalse(parser.isCreditTransaction("Your recharge pack expires today"))
    }

    @Test
    fun extractsUpiAndBankingReferenceNumbers() {
        val parser = SmsParser()
        val ref1 = parser.extractReferenceNumber("A/c *1234 credited by Rs 500.00 on 27-Oct-26 by UPI Ref 123456789012 - SBI")
        assertEquals("123456789012", ref1)

        val ref2 = parser.extractReferenceNumber("Sent Rs 1500 to Rahul. UTR: SBIN000123456")
        assertEquals("SBIN000123456", ref2)

        val ref3 = parser.extractReferenceNumber("Txn ID: TXN99887766 credited")
        assertEquals("TXN99887766", ref3)
    }

    @Test
    fun testFamPayCreditAndSenderExtraction() {
        val parser = SmsParser()
        val msg1 = "₹500 received from Rahul Sharma on FamPay #fampaid"
        assertTrue(parser.isCreditTransaction(msg1))
        assertEquals("500", parser.extractAmount(msg1))
        assertEquals("Rahul Sharma", parser.getSenderName(msg1))

        val msg2 = "#fampaid: Amit Verma sent ₹750 via FamPay"
        assertTrue(parser.isCreditTransaction(msg2))
        assertEquals("750", parser.extractAmount(msg2))
        assertEquals("Amit Verma", parser.getSenderName(msg2))

        val msg3 = "Pooja Patel paid you ₹200 on FamPay #fampaid"
        assertTrue(parser.isCreditTransaction(msg3))
        assertEquals("200", parser.extractAmount(msg3))
        assertEquals("Pooja Patel", parser.getSenderName(msg3))
    }

    @Test
    fun testPrecisionForensicParserEliminatesFalsePositives() {
        val parser = SmsParser()
        // Loan alerts
        assertFalse(parser.isCreditTransaction("Pre-approved personal loan of Rs 5,00,000 disbursed to your account"))
        assertFalse(parser.isCreditTransaction("Instant loan offer! Rs 50,000 sanctioned for your A/c. Apply now."))
        assertFalse(parser.isCreditTransaction("Your EMI of Rs 4,500 is due on 05-Nov for personal loan"))

        // Coins & Rewards
        assertFalse(parser.isCreditTransaction("500 SuperCoins credited to your wallet for shopping"))
        assertFalse(parser.isCreditTransaction("Congratulations! 200 Reward Points added to your account"))
        assertFalse(parser.isCreditTransaction("You won a scratch card! Cashback points credited"))

        // LPG Bookings
        assertFalse(parser.isCreditTransaction("Indane LPG cylinder booked for Rs 850. Ref: 987654"))
        assertFalse(parser.isCreditTransaction("Bharat Gas refill booking confirmed for Rs 910"))
        assertFalse(parser.isCreditTransaction("HP Gas cylinder booked successfully for Rs 860"))

        // Telco / Utility / OTP
        assertFalse(parser.isCreditTransaction("Your electricity bill payment due for Rs 1200"))
        assertFalse(parser.isCreditTransaction("Data pack recharge of Rs 299 expiring in 2 days"))
        assertFalse(parser.isCreditTransaction("Your OTP for UPI registration is 459102. Do not share."))
    }

    @Test
    fun testAnnouncementPhrasesSegmentedFormatWithPipes() {
        val phraseEn = AnnouncementPhrases.buildPhrase("English", "|500|", null, "Rahul")
        assertEquals("Received |500| rupees from Rahul", phraseEn)

        val phraseHi = AnnouncementPhrases.buildPhrase("Hindi", "|250|", null, "Amit")
        assertEquals("Amit से |250| रुपये प्राप्त हुए", phraseHi)
    }
}
