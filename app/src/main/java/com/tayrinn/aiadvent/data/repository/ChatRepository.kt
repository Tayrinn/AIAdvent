package com.tayrinn.aiadvent.data.repository

import com.tayrinn.aiadvent.data.api.OllamaApi
import com.tayrinn.aiadvent.data.database.ChatMessageDao
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.model.OllamaRequest
import com.tayrinn.aiadvent.data.model.OllamaOptions
import com.tayrinn.aiadvent.data.model.OllamaChatRequest
import com.tayrinn.aiadvent.data.model.OllamaMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val ollamaApi: OllamaApi
) {
    fun getAllMessages(): Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun insertMessage(message: ChatMessage) = chatMessageDao.insertMessage(message)

    suspend fun deleteMessage(message: ChatMessage) = chatMessageDao.deleteMessage(message)

    suspend fun deleteAllMessages() = chatMessageDao.deleteAllMessages()

    suspend fun sendMessageToOllama(
        content: String,
        conversationHistory: List<ChatMessage>
    ): Result<String> {
        return try {
            // Формируем контекст из истории сообщений для Ollama
            val context = buildString {
                conversationHistory.forEach { message ->
                    val role = if (message.isUser) "User" else "Assistant"
                    appendLine("$role: ${message.content}")
                }
                append("User: $content")
                appendLine("\nAssistant:")
            }

            // Сначала пробуем чат-модель
            val response = try {
                val messages = conversationHistory.map { message ->
                    OllamaMessage(
                        role = if (message.isUser) "user" else "assistant",
                        content = message.content
                    )
                }.toMutableList()
                
                messages.add(OllamaMessage(role = "user", content = content))
                
                val chatRequest = OllamaChatRequest(
                    model = "llama2",
                    messages = messages,
                    options = OllamaOptions(
                        temperature = 0.7,
                        top_p = 0.9,
                        num_predict = 150
                    )
                )
                
                val chatResponse = ollamaApi.generateChat(chatRequest)
                chatResponse.message.content
                
            } catch (e: Exception) {
                // Если чат-модель недоступна, используем генерацию текста
                val request = OllamaRequest(
                    model = "llama2",
                    prompt = context,
                    options = OllamaOptions(
                        temperature = 0.7,
                        top_p = 0.9,
                        num_predict = 150
                    )
                )
                
                val textResponse = ollamaApi.generateText(request)
                textResponse.response
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
