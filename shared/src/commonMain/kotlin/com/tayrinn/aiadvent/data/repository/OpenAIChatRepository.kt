package com.tayrinn.aiadvent.data.repository

import com.tayrinn.aiadvent.data.api.OpenAIApi
import com.tayrinn.aiadvent.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for chat functionality using OpenAI ChatGPT API
 * Simplified version without Room database for desktop app
 */
class OpenAIChatRepository(
    private val openAIApi: OpenAIApi
) {
    
    // In-memory storage for messages (простое решение для desktop версии)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
    
    private var nextId = 1L
    
    /**
     * Send message to OpenAI ChatGPT and get response
     */
    suspend fun sendMessage(content: String): Pair<String, String> {
        return try {
            openAIApi.sendMessage(content)
        } catch (e: Exception) {
            println("Error in OpenAIChatRepository.sendMessage: ${e.message}")
            Pair(
                "Ошибка при отправке сообщения к Hugging Face: ${e.message}",
                "Проверьте подключение к интернету"
            )
        }
    }
    
    /**
     * Add message to in-memory storage
     */
    fun insertMessage(message: ChatMessage) {
        val messageWithId = message.copy(id = nextId++)
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(messageWithId)
        _messages.value = currentMessages
    }
    
    /**
     * Get all messages from in-memory storage
     */
    fun getAllMessages(): Flow<List<ChatMessage>> = messages
    
    /**
     * Clear all messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
        nextId = 1L
    }
    
    /**
     * Load messages (for compatibility - no-op for in-memory storage)
     */
    fun loadMessages() {
        // No-op for in-memory storage
    }
    
    /**
     * Get current messages list
     */
    fun getCurrentMessages(): List<ChatMessage> = _messages.value
    
    /**
     * Simple API limits for OpenAI (mock data)
     */
    suspend fun getApiLimits(): Result<com.tayrinn.aiadvent.data.model.ApiLimits> {
        return Result.success(
            com.tayrinn.aiadvent.data.model.ApiLimits(
                remainingGenerations = 100,
                totalGenerations = 100,
                resetDate = "2024-12-31"
            )
        )
    }
    
    /**
     * Generate image (placeholder - not implemented for OpenAI text model)
     */
    suspend fun generateImage(prompt: String): Result<String> {
        return Result.failure(Exception("Генерация изображений не поддерживается в ChatGPT интеграции"))
    }
}
