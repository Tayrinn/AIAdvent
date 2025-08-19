package com.tayrinn.aiadvent.data.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ModelTest {
    
    @Test
    fun `test ChatMessage creation`() {
        // Given
        val content = "Test message"
        val isUser = true
        val isAgent1 = false
        val isAgent2 = false
        
        // When
        val message = ChatMessage(
            content = content,
            isUser = isUser,
            isAgent1 = isAgent1,
            isAgent2 = isAgent2
        )
        
        // Then
        assertEquals(content, message.content)
        assertEquals(isUser, message.isUser)
        assertEquals(isAgent1, message.isAgent1)
        assertEquals(isAgent2, message.isAgent2)
        assertFalse(message.isAgent1)
        assertFalse(message.isAgent2)
    }
    
    @Test
    fun `test ChatMessage with agent1`() {
        // Given
        val content = "Agent 1 message"
        
        // When
        val message = ChatMessage(
            content = content,
            isUser = false,
            isAgent1 = true,
            isAgent2 = false
        )
        
        // Then
        assertEquals(content, message.content)
        assertFalse(message.isUser)
        assertTrue(message.isAgent1)
        assertFalse(message.isAgent2)
    }
    
    @Test
    fun `test ChatMessage with agent2`() {
        // Given
        val content = "Agent 2 message"
        
        // When
        val message = ChatMessage(
            content = content,
            isUser = false,
            isAgent1 = false,
            isAgent2 = true
        )
        
        // Then
        assertEquals(content, message.content)
        assertFalse(message.isUser)
        assertFalse(message.isAgent1)
        assertTrue(message.isAgent2)
    }
    
    @Test
    fun `test ApiLimits creation`() {
        // Given
        val remaining = 95
        val total = 100
        
        // When
        val limits = ApiLimits(
            remainingGenerations = remaining,
            totalGenerations = total
        )
        
        // Then
        assertEquals(remaining, limits.remainingGenerations)
        assertEquals(total, limits.totalGenerations)
    }
    
    @Test
    fun `test ApiLimits percentage calculation`() {
        // Given
        val remaining = 75
        val total = 100
        
        // When
        val limits = ApiLimits(
            remainingGenerations = remaining,
            totalGenerations = total
        )
        
        // Then
        assertEquals(75, limits.remainingGenerations)
        assertEquals(100, limits.totalGenerations)
        assertEquals(25.0f, limits.usagePercentage, 0.01f)
    }
    
    @Test
    fun `test ApiLimits with zero remaining`() {
        // Given
        val remaining = 0
        val total = 100
        
        // When
        val limits = ApiLimits(
            remainingGenerations = remaining,
            totalGenerations = total
        )
        
        // Then
        assertEquals(0, limits.remainingGenerations)
        assertEquals(100, limits.totalGenerations)
        assertEquals(100.0f, limits.usagePercentage, 0.01f)
    }
}
