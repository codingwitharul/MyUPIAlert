package com.codingwitharul.myupialert.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.codingwitharul.myupialert.ui.screens.SettingsDrawerContent
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class SettingsDrawerContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsDrawerContent_displaysAllOptions() {
        val state = MainState(
            isMuted = false,
            announceSenderName = true,
            language = "English",
            volume = 75,
            speedInt = 5,
            speedFloat = 1.0f,
            forwarderNumber = "",
            forwarderAppFilter = "all",
            forwarderTypeFilter = "Both"
        )
        
        var receivedIntent: MainIntent? = null

        composeTestRule.setContent {
            SettingsDrawerContent(
                state = state,
                onIntent = { receivedIntent = it },
                onClose = { }
            )
        }

        // Verify standard labels exist
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language (TTS)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mute Announcements").assertIsDisplayed()
        composeTestRule.onNodeWithText("Announce Sender Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Speech Speed").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS Forwarder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Sounds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export Transactions").assertIsDisplayed()
    }
    
    @Test
    fun testSettingsDrawerContent_testDefaultSoundEmitsIntent() {
        var receivedIntent: MainIntent? = null

        composeTestRule.setContent {
            SettingsDrawerContent(
                state = MainState(),
                onIntent = { receivedIntent = it },
                onClose = { }
            )
        }
        
        composeTestRule.onNodeWithText("Test Default Sound").performClick()
        
        assertTrue(receivedIntent is MainIntent.TestSound)
        val testSoundIntent = receivedIntent as MainIntent.TestSound
        assertTrue(testSoundIntent.amount == null)
    }
}
