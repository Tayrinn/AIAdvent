package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.OllamaRequest
import com.tayrinn.aiadvent.data.model.OllamaResponse
import com.tayrinn.aiadvent.data.model.OllamaChatRequest
import com.tayrinn.aiadvent.data.model.OllamaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    // Основной метод для генерации текста
    @POST("api/generate")
    suspend fun generateText(
        @Body request: OllamaRequest
    ): OllamaResponse
    
    // Метод для чат-моделей
    @POST("api/chat")
    suspend fun generateChat(
        @Body request: OllamaChatRequest
    ): OllamaChatResponse
    
    // Альтернативная модель (если llama2 недоступна)
    @POST("api/generate")
    suspend fun generateWithMistral(
        @Body request: OllamaRequest
    ): OllamaResponse
}
