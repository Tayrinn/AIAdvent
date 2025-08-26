package com.tayrinn.aiadvent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String = "gpt-5",
    val messages: List<OpenAIMessage>,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = 2000,
    val stream: Boolean = false
)

@Serializable
data class OpenAIMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

@Serializable
data class OpenAIError(
    val error: OpenAIErrorDetails
)

@Serializable
data class OpenAIErrorDetails(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null
)
