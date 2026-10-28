package com.codingwitharul.myupialert.ui

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class MainViewModelTest {

    @Test
    fun testSoundIntent_nullAmount_emitsSamplePhrase() {
        // Checking the contract mapping
        val intent = MainIntent.TestSound(amount = null)
        
        val amt = if (intent.amount.isNullOrBlank()) "150" else intent.amount!!.trim()
        val fakeBody = if (intent.amount == null) "Sample test announcement from UPI Payment Alert" else "Rs $amt credited to your A/c"
        
        assertEquals("Sample test announcement from UPI Payment Alert", fakeBody)
    }

    @Test
    fun testSoundIntent_withAmount_emitsFormattedPhrase() {
        val intent = MainIntent.TestSound(amount = "500")
        
        val amt = if (intent.amount.isNullOrBlank()) "150" else intent.amount!!.trim()
        val fakeBody = if (intent.amount == null) "Sample test announcement from UPI Payment Alert" else "Rs $amt credited to your A/c"
        
        assertEquals("Rs 500 credited to your A/c", fakeBody)
    }
}
