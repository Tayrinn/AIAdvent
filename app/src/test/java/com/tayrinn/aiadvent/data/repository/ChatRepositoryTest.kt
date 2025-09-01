package com.tayrinn.aiadvent.data.repository

import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.model.ApiLimits
import com.tayrinn.aiadvent.data.database.ChatMessageDao
import com.tayrinn.aiadvent.data.api.OllamaApi
import com.tayrinn.aiadvent.data.service.ImageGenerationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {
    
    private lateinit var repository: ChatRepository
    private lateinit var mockChatDao: ChatMessageDao
    private lateinit var mockOllamaApi: OllamaApi
    private lateinit var mockImageGenerationService: ImageGenerationService
    private lateinit var mockContext: android.content.Context
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChatDao = mock()
        mockOllamaApi = mock()
        mockImageGenerationService = mock()
        mockContext = mock()
        repository = ChatRepository(mockOllamaApi, mockChatDao, mockImageGenerationService, mockContext)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test insertMessage success`() = runTest {
        // Given
        val message = ChatMessage(content = "Test message", isUser = true)
        
        // When
        repository.insertMessage(message)
        
        // Then
        verify(mockChatDao, times(1)).insertMessage(message)
    }
    
    @Test
    fun `test getAllMessages success`() = runTest {
        // Given
        val testMessages = listOf(
            ChatMessage(content = "Message 1", isUser = true),
            ChatMessage(content = "Message 2", isUser = false, isAgent1 = true)
        )
        whenever(mockChatDao.getAllMessages()).thenReturn(flowOf(testMessages))
        
        // When
        val result = repository.getAllMessages()
        
        // Then
        result.collect { messages ->
            assertEquals(testMessages, messages)
        }
    }
    
    @Test
    fun `test sendMessage success`() = runTest {
        // Given
        val userMessage = "Hello"
        val currentMessages = listOf(ChatMessage(content = "Previous", isUser = false, isAgent1 = true))
        val agent1Response = "Agent 1 response"
        val agent2Response = "Agent 2 response"
        
        whenever(mockOllamaApi.agent1Generate(any())).thenReturn(com.tayrinn.aiadvent.data.model.OllamaResponse(
            model = "phi3",
            created_at = "2025-01-01T00:00:00Z",
            response = agent1Response,
            done = true
        ))
        whenever(mockOllamaApi.agent2Generate(any())).thenReturn(com.tayrinn.aiadvent.data.model.OllamaResponse(
            model = "llama2",
            created_at = "2025-01-01T00:00:00Z",
            response = agent2Response,
            done = true
        ))
        
        // When
        val result = repository.sendMessage(userMessage, currentMessages, null)
        
        // Then
        assertEquals(agent1Response, result.first)
        assertEquals(agent2Response, result.second)
        verify(mockChatDao, times(2)).insertMessage(any())
    }
    
    @Test
    fun `test clearMessages success`() = runTest {
        // When
        repository.clearMessages()
        
        // Then
        verify(mockChatDao, times(1)).deleteAllMessages()
    }
    
    @Test
    fun `test getApiLimits success`() = runTest {
        // Given
        val expectedLimits = ApiLimits(remainingGenerations = 90, totalGenerations = 100)
        whenever(mockImageGenerationService.getApiLimits()).thenReturn(kotlin.Result.success(expectedLimits))
        
        // When
        val result = repository.getApiLimits()
        
        // Then
        assertEquals(expectedLimits, result)
    }
    
    @Test
    fun `test generateImage success`() = runTest {
        // Given
        val prompt = "Generate a cat image"
        val expectedImageUrl = "http://example.com/cat.jpg"
        whenever(mockImageGenerationService.generateImage(prompt, any())).thenReturn(kotlin.Result.success(expectedImageUrl))
        
        // When
        val result = repository.generateImage(prompt)
        
        // Then
        assertEquals(expectedImageUrl, result.getOrNull())
    }
}
