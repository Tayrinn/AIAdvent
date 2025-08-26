package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.OpenAIApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TestGenerationServiceLogTest {
    
    @Test
    fun `test generateTests logging`() = runTest {
        // Arrange
        val mockOpenAIApi = mock(OpenAIApi::class.java)
        val testGenerationService = TestGenerationService(mockOpenAIApi)
        
        val sourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val fileName = "TestFile.kt"
        
        // Mock response
        val mockResponse = mock(com.tayrinn.aiadvent.data.model.OpenAIResponse::class.java)
        val mockChoice = mock(com.tayrinn.aiadvent.data.model.OpenAIChoice::class.java)
        val mockMessage = mock(com.tayrinn.aiadvent.data.model.OpenAIMessage::class.java)
        
        whenever(mockMessage.content).thenReturn("""
            @Test
            fun testAdd() {
                assertEquals(3, add(1, 2))
            }
        """.trimIndent())
        
        whenever(mockChoice.message).thenReturn(mockMessage)
        whenever(mockResponse.choices).thenReturn(listOf(mockChoice))
        whenever(mockOpenAIApi.chatCompletion(any())).thenReturn(mockResponse)
        
        // Act
        val result = testGenerationService.generateTests(sourceCode, fileName)
        
        // Assert
        assertNotNull(result)
        assertFalse(result.isEmpty())
        
        // Verify that the API was called
        verify(mockOpenAIApi).chatCompletion(any())
    }
}
