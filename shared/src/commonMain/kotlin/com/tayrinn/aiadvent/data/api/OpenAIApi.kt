package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.OpenAIRequest
import com.tayrinn.aiadvent.data.model.OpenAIResponse

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
     * @param recentMessages List of recent messages for context (last 3)
     * @param modelName Name of the AI model to use
     * @return Pair of responses (agent1, agent2) for compatibility
     */
    suspend fun sendMessage(
        message: String,
        recentMessages: List<com.tayrinn.aiadvent.data.model.ChatMessage> = emptyList(),
        modelName: String? = null
    ): Pair<String, String>
}

/**
 * Implementation of OpenAI API using HTTP client
 */
expect class OpenAIApiImpl() : OpenAIApi

/**
 * Implementation of Hugging Face API
 */
expect fun createHuggingFaceApi(): HuggingFaceApi
