package com.tayrinn.aiadvent.data.repository

import com.tayrinn.aiadvent.data.api.OllamaApi
import com.tayrinn.aiadvent.data.database.ChatMessageDao
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.model.OllamaRequest
import com.tayrinn.aiadvent.data.model.OllamaOptions
import com.tayrinn.aiadvent.data.model.ApiLimits
import com.tayrinn.aiadvent.data.service.ImageGenerationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Context

class ChatRepository(
    private val ollamaApi: OllamaApi,
    private val chatMessageDao: ChatMessageDao,
    private val imageGenerationService: ImageGenerationService,
    private val context: Context
) {
    
    // Системные сообщения для двух агентов
    private val agent1SystemMessage = """You are Agent 1 - a helpful AI assistant. Your task:
    
1. PRIMARY RESPONSE: Provide clear, detailed, and comprehensive answers to user questions
2. EXPERTISE: Use your knowledge to give valuable insights and solutions
3. CLARITY: Make your responses easy to understand and well-structured
4. COMPLETENESS: Cover the topic thoroughly but concisely

Rules:
- Give direct, helpful answers
- Be informative and engaging
- Use natural, conversational language
- Respond in English only"""

    private val agent2SystemMessage = """You are Agent 2 - a refinement specialist. Your task:
    
1. ENHANCE: Take Agent 1's response and improve it
2. CLARIFY: Add missing details or clarify unclear points
3. EXPAND: Provide additional context, examples, or related information
4. CORRECT: Fix any inaccuracies or suggest better approaches

Rules:
- Always reference what Agent 1 said
- Add value, don't just repeat
- Be constructive and helpful
- Keep responses concise but insightful
- Respond in English only"""
    
    suspend fun sendMessage(content: String, conversationHistory: List<ChatMessage>): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем, является ли сообщение запросом на генерацию изображения
                if (isImageGenerationRequest(content)) {
                    return@withContext Pair("", "") // Обрабатывается отдельно в ViewModel
                }
                
                // Агент 1: Отвечает на вопрос пользователя
                val agent1Prompt = buildString {
                    appendLine(agent1SystemMessage)
                    appendLine()
                    appendLine("User question: $content")
                    appendLine()
                    appendLine("Previous conversation context:")
                    conversationHistory.takeLast(10).forEach { message ->
                        appendLine("${if (message.isUser) "User" else "AI"}: ${message.content}")
                    }
                }

                val agent1Request = OllamaRequest(
                    model = "phi3",
                    prompt = agent1Prompt,
                    options = OllamaOptions(
                        temperature = 0.3,
                        top_p = 0.7,
                        num_predict = 256,
                        top_k = 20
                    )
                )

                val agent1Response = try {
                    ollamaApi.agent1Generate(agent1Request).response
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }

                // Агент 2: Дополняет и улучшает ответ Агента 1
                val agent2Prompt = buildString {
                    appendLine(agent2SystemMessage)
                    appendLine()
                    appendLine("User question: $content")
                    appendLine()
                    appendLine("Agent 1's response: $agent1Response")
                    appendLine()
                    appendLine("Your task: Enhance, clarify, and improve Agent 1's response.")
                }

                val agent2Request = OllamaRequest(
                    model = "llama2",
                    prompt = agent2Prompt,
                    options = OllamaOptions(
                        temperature = 0.4,
                        top_p = 0.8,
                        num_predict = 200,
                        top_k = 25
                    )
                )

                val agent2Response = try {
                    ollamaApi.agent2Generate(agent2Request).response
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }

                Pair(agent1Response, agent2Response)
            } catch (e: Exception) {
                Pair("Error: ${e.message}", "Error: ${e.message}")
            }
        }
    }
    
    suspend fun generateImage(prompt: String, style: String = "DEFAULT"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                imageGenerationService.generateImage(prompt, style)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun isImageGenerationRequest(content: String): Boolean {
        val lowerContent = content.lowercase()
        return lowerContent.contains("сгенерируй") && 
               (lowerContent.contains("изображение") || lowerContent.contains("картинку") || lowerContent.contains("рисунок")) ||
               lowerContent.contains("generate") && 
               (lowerContent.contains("image") || lowerContent.contains("picture") || lowerContent.contains("drawing"))
    }
    
    suspend fun insertMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message)
    }
    
    fun getAllMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages()
    }
    
                    suspend fun getAllMessagesSync(): List<ChatMessage> {
                    Log.d("ChatRepository", "getAllMessagesSync called")
                    return withContext(Dispatchers.IO) {
                        try {
                            Log.d("ChatRepository", "Inside withContext(Dispatchers.IO), thread: ${Thread.currentThread().name}")
                            Log.d("ChatRepository", "About to call chatMessageDao.getAllMessagesSync()")
                            val messages = chatMessageDao.getAllMessagesSync()
                            Log.d("ChatRepository", "Successfully received ${messages.size} messages from DAO")
                            messages
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error in getAllMessagesSync: ${e.message}")
                            throw e
                        }
                    }
                }
                
                suspend fun getApiLimits(): ApiLimits? {
                    return withContext(Dispatchers.IO) {
                        try {
                            val result = imageGenerationService.getApiLimits()
                            result.getOrNull()
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error getting API limits: ${e.message}")
                            null
                        }
                    }
                }
                
                suspend fun decreaseApiLimits() {
                    withContext(Dispatchers.IO) {
                        try {
                            imageGenerationService.decreaseRemainingGenerations()
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error decreasing API limits: ${e.message}")
                        }
                    }
                }
    
    suspend fun clearMessages() {
        chatMessageDao.deleteAllMessages()
    }
    
    fun getContext(): Context {
        return context
    }
}
