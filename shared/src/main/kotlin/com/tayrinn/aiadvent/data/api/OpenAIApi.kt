package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.OpenAIRequest
import com.tayrinn.aiadvent.data.model.OpenAIResponse
import com.tayrinn.aiadvent.data.model.ChatMessage

/**
 * Retrofit interface for OpenAI ChatGPT API
 */
interface OpenAIApi {
    
    /**
     * Send a chat completion request to OpenAI
     * @param request The chat completion request with messages
     * @return OpenAI response with generated message
     */
    suspend fun chatCompletion(
        request: OpenAIRequest
    ): OpenAIResponse
    
    /**
     * Send a message and get response from ChatGPT
     * @param message User message content
     * @param conversationHistory Previous messages for context
     * @param maxTokensParam Maximum tokens for response (null for default)
     * @return Pair of responses (agent1, agent2) for compatibility
     */
    suspend fun sendMessage(
        message: String, 
        conversationHistory: List<ChatMessage> = emptyList(),
        maxTokensParam: Int? = null
    ): Pair<String, String>
}

/**
 * Factory function to create OpenAI API implementation
 */
fun createOpenAIApiImpl(): OpenAIApi = OpenAIApiImplInternal()
