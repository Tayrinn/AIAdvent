package com.tayrinn.aiadvent

import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.model.ApiLimits
import org.junit.Test
import org.junit.Assert.*

class SimpleTest {
    
    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            content = "Hello, world!",
            isUser = true
        )
        
        assertEquals("Hello, world!", message.content)
        assertTrue(message.isUser)
        assertFalse(message.isAgent1)
        assertFalse(message.isAgent2)
        assertFalse(message.isError)
        assertFalse(message.isImageGeneration)
        assertFalse(message.isTestReport)
    }
    
    @Test
    fun testApiLimitsCreation() {
        val limits = ApiLimits(
            remainingGenerations = 80,
            totalGenerations = 100
        )
        
        assertEquals(80, limits.remainingGenerations)
        assertEquals(100, limits.totalGenerations)
        assertEquals(20, limits.usedGenerations)
        assertEquals(20.0f, limits.usagePercentage, 0.01f)
        assertEquals(80.0f, limits.remainingPercentage, 0.01f)
        assertFalse(limits.isLimitReached)
    }
    
    @Test
    fun testApiLimitsWithZeroRemaining() {
        val limits = ApiLimits(
            remainingGenerations = 0,
            totalGenerations = 100
        )
        
        assertEquals(0, limits.remainingGenerations)
        assertEquals(100, limits.totalGenerations)
        assertEquals(100, limits.usedGenerations)
        assertEquals(100.0f, limits.usagePercentage, 0.01f)
        assertEquals(0.0f, limits.remainingPercentage, 0.01f)
        assertTrue(limits.isLimitReached)
    }
    
    @Test
    fun testImageGenerationDetection() {
        val imageRequests = listOf(
            "Generate an image of a cat",
            "Create a picture of a dog",
            "Make an image of a bird",
            "Draw a cat",
            "Show me a picture of a tree"
        )
        
        val regularRequests = listOf(
            "Hello, how are you?",
            "What's the weather like?",
            "Tell me a joke",
            "How do I cook pasta?"
        )
        
        imageRequests.forEach { request ->
            assertTrue("Should detect image request: $request", isImageGenerationRequest(request))
        }
        
        regularRequests.forEach { request ->
            assertFalse("Should not detect image request: $request", isImageGenerationRequest(request))
        }
    }
    
    private fun isImageGenerationRequest(text: String): Boolean {
        val lowerText = text.lowercase()
        return lowerText.contains("generate") || 
               lowerText.contains("create") || 
               lowerText.contains("make") || 
               lowerText.contains("draw") || 
               lowerText.contains("picture") || 
               lowerText.contains("image")
    }
}
