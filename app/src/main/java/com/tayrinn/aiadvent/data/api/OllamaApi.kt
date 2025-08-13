package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.OllamaRequest
import com.tayrinn.aiadvent.data.model.OllamaResponse
import com.tayrinn.aiadvent.data.model.OllamaChatRequest
import com.tayrinn.aiadvent.data.model.OllamaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("api/chat")
    suspend fun generateChat(
        @Body request: OllamaChatRequest
    ): OllamaChatResponse

    @POST("api/generate")
    suspend fun generateText(@Body request: OllamaRequest): OllamaResponse

    // API для двух агентов - используем существующие типы
    @POST("api/generate")
    suspend fun agent1Generate(@Body request: OllamaRequest): OllamaResponse

    @POST("api/generate")
    suspend fun agent2Generate(@Body request: OllamaRequest): OllamaResponse
}
