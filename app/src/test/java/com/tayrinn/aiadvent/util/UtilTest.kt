package com.tayrinn.aiadvent.util

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class UtilTest {
    
    @Test
    fun `test isImageGenerationRequest with various inputs`() {
        // Given & When & Then
        assertTrue("Generate an image of a cat".isImageGenerationRequest())
        assertTrue("Create a picture of a dog".isImageGenerationRequest())
        assertTrue("Make an image of a bird".isImageGenerationRequest())
        assertTrue("Draw a cat".isImageGenerationRequest())
        assertTrue("Show me a picture of a tree".isImageGenerationRequest())
        
        assertFalse("Hello, how are you?".isImageGenerationRequest())
        assertFalse("What's the weather like?".isImageGenerationRequest())
        assertFalse("Tell me a joke".isImageGenerationRequest())
        assertFalse("".isImageGenerationRequest())
        assertFalse("   ".isImageGenerationRequest())
    }
    
    @Test
    fun `test isImageGenerationRequest case insensitive`() {
        // Given & When & Then
        assertTrue("GENERATE AN IMAGE".isImageGenerationRequest())
        assertTrue("Create A Picture".isImageGenerationRequest())
        assertTrue("make an image".isImageGenerationRequest())
        assertTrue("DRAW A CAT".isImageGenerationRequest())
    }
    
    @Test
    fun `test isImageGenerationRequest with mixed content`() {
        // Given & When & Then
        assertTrue("Hello, can you generate an image of a cat?".isImageGenerationRequest())
        assertTrue("I would like to create a picture of a dog, please".isImageGenerationRequest())
        assertFalse("I want to chat about images, but not generate them".isImageGenerationRequest())
    }
}

// Extension function for testing
private fun String.isImageGenerationRequest(): Boolean {
    val lowerText = this.lowercase()
    return lowerText.contains("generate") || 
           lowerText.contains("create") || 
           lowerText.contains("make") || 
           lowerText.contains("draw") || 
           lowerText.contains("picture") || 
           lowerText.contains("image")
}
