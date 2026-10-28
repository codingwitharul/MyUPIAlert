package com.codingwitharul.myupialert.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codingwitharul.myupialert.smsparser.SmsParser
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.java.KoinJavaComponent

@RunWith(AndroidJUnit4::class)
class SmsParserKoinTest {

    @Test
    fun testSmsParser_canBeInjectedAndExtractsAmount() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Ensure Koin is running
        val koin = GlobalContext.getOrNull()
        assertNotNull("Koin is not initialized. Ensure MyUPIAlertApp started it.", koin)
        
        val smsParser = KoinJavaComponent.getKoin().get<SmsParser>()
        assertNotNull(smsParser)
        
        val phrase = smsParser.getAmountFromMessageBody(appContext, "Rs 1500 credited to your A/c", "English", true, "Test User")
        assertTrue("Phrase should contain rupees", phrase.contains("rupees", ignoreCase = true) || phrase.contains("1500"))
    }
}
