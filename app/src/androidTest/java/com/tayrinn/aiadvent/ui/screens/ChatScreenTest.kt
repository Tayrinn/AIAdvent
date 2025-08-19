package com.tayrinn.aiadvent.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tayrinn.aiadvent.ui.theme.AIAdventTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testChatScreenInitialState() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("AIAdvent").assertExists()
        composeTestRule.onNodeWithText("🚀 **FIRST LAUNCH TEST:** Database cleared and ready!").assertExists()
    }
    
    @Test
    fun testInputFieldExists() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Type a message...").assertExists()
    }
    
    @Test
    fun testSendButtonExists() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Send").assertExists()
    }
    
    @Test
    fun testSettingsButtonExists() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }
    
    @Test
    fun testInputFieldIsEditable() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Type a message...").performTextInput("Hello, world!")
        
        // Then
        composeTestRule.onNodeWithText("Hello, world!").assertExists()
    }
    
    @Test
    fun testEmptyInputValidation() {
        // Given
        composeTestRule.setContent {
            AIAdventTheme {
                ChatScreen()
            }
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        
        // Then - should not crash and input field should remain empty
        composeTestRule.onNodeWithText("Type a message...").assertExists()
    }
}
