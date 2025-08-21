import org.junit.Test
import org.junit.Assert.*

class SimpleTest {
    
    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            content = "Test message",
            isUser = true
        )
        
        assertEquals("Test message", message.content)
        assertTrue(message.isUser)
        assertFalse(message.isError)
        assertFalse(message.isTestReport)
    }
    
    @Test
    fun testApiLimitsCalculations() {
        val limits = ApiLimits(
            remainingGenerations = 75,
            totalGenerations = 100
        )
        
        assertEquals(25, limits.usedGenerations)
        assertEquals(25.0f, limits.usagePercentage, 0.01f)
        assertEquals(75.0f, limits.remainingPercentage, 0.01f)
        assertFalse(limits.isLimitReached)
    }
    
    @Test
    fun testImageGenerationRequestDetection() {
        val imageRequests = listOf(
            "Generate an image of a cat",
            "Create a picture of a dog",
            "Make an image of a bird"
        )
        
        val regularRequests = listOf(
            "Hello, how are you?",
            "What's the weather like?",
            "Tell me a joke"
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
