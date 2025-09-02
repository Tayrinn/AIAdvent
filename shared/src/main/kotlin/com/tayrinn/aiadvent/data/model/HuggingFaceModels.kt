package com.tayrinn.aiadvent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceRequest(
    val messages: List<HuggingFaceMessage>,
    val model: String = "deepseek-ai/DeepSeek-V3-0324"
)

@Serializable
data class HuggingFaceMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class HuggingFaceResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<HuggingFaceChoice>,
    val usage: HuggingFaceUsage? = null
)

@Serializable
data class HuggingFaceChoice(
    val index: Int,
    val message: HuggingFaceMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class HuggingFaceUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

@Serializable
data class HuggingFaceError(
    val error: HuggingFaceErrorDetails
)

@Serializable
data class HuggingFaceErrorDetails(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null
)

// Модели для Whisper (распознавание речи)
@Serializable
data class WhisperResponse(
    val text: String
)

// Модели для TTS (синтез речи)
@Serializable
data class TTSRequest(
    val text: String
)

@Serializable
data class TTSResponse(
    val audio: String // base64 encoded audio
)
