package com.tayrinn.aiadvent.data.model

import com.google.gson.annotations.SerializedName

data class OllamaRequest(
    val model: String = "phi3",
    val prompt: String,
    val stream: Boolean = false,
    val options: OllamaOptions = OllamaOptions()
)

data class OllamaOptions(
    val temperature: Double = 0.7,
    val top_p: Double = 0.9,
    val top_k: Int = 40,
    val num_predict: Int = 512
)

data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean,
    val context: List<Int>? = null,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Int? = null,
    val eval_duration: Long? = null
)

// Для чат-моделей
data class OllamaChatRequest(
    val model: String = "phi3",
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions = OllamaOptions()
)

data class OllamaMessage(
    val role: String,
    val content: String
)

data class OllamaChatResponse(
    val model: String,
    val created_at: String,
    val message: OllamaMessage,
    val done: Boolean,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Int? = null,
    val eval_duration: Long? = null
)
