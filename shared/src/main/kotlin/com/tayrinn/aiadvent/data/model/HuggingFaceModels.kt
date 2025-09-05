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

// === User Preferences Models ===

@Serializable
data class UserPreferences(
    val userId: String,
    val language: String = "ru", // Язык общения (ru, en, etc.)
    val name: String? = null, // Имя пользователя
    val communicationStyle: String = "friendly", // Стиль общения (formal, friendly, casual)
    val responseLength: String = "medium", // Длина ответов (short, medium, long)
    val interests: List<String> = emptyList(), // Интересы пользователя
    val expertise: List<String> = emptyList(), // Области экспертизы пользователя
    val timezone: String? = null, // Часовой пояс
    val preferredTopics: List<String> = emptyList(), // Предпочитаемые темы
    val avoidTopics: List<String> = emptyList(), // Темы, которых следует избегать
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class PreferenceExtractionRequest(
    val messages: List<ChatMessage>,
    val currentPreferences: UserPreferences?,
    val model: String
)

@Serializable
data class PreferenceExtractionResponse(
    val extractedPreferences: UserPreferences,
    val confidence: Float, // Уверенность в извлеченных предпочтениях (0.0 - 1.0)
    val changes: List<String> // Список изменений, которые были обнаружены
)
