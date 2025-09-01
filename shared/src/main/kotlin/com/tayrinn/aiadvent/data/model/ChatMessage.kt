package com.tayrinn.aiadvent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isAgent1: Boolean = false,
    val isAgent2: Boolean = false,
    val isImageGeneration: Boolean = false,
    val isTestReport: Boolean = false,
    val imageUrl: String? = null,
    val imagePrompt: String? = null
)
