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
        var attempts = 0
        val maxAttempts = 3
        
        while (attempts < maxAttempts) {
            attempts++
            try {
                // Системное сообщение для JSON-ответа
                val systemMessage = OllamaMessage(
                    role = "system",
                    content = "Generate ONLY valid JSON. No markdown, no comments, no explanations. Start with { and end with }. Pure JSON only."
                )

                // Добавляем пример JSON в user prompt, если его нет
                val userPrompt = if (content.contains("{")) {
                    content
                } else {
                    "$content\nExample response: {\"key\": \"value\"}"
                }

                // Только system + user message (без истории)
                val messages = mutableListOf<OllamaMessage>()
                messages.add(systemMessage)
                messages.add(OllamaMessage(role = "user", content = userPrompt))

                val chatRequest = OllamaChatRequest(
                    model = "phi3",
                    messages = messages,
                    options = OllamaOptions(
                        temperature = 0.7,
                        top_p = 0.9,
                        num_predict = 512
                    )
                )

                val response = try {
                    val chatResponse = ollamaApi.generateChat(chatRequest)
                    chatResponse.message.content
                } catch (e: Exception) {
                    // Если чат-модель недоступна, используем генерацию текста
                    val request = OllamaRequest(
                        model = "phi3",
                        prompt = userPrompt,
                        options = OllamaOptions(
                            temperature = 0.7,
                            top_p = 0.9,
                            num_predict = 512
                        )
                    )
                    val textResponse = ollamaApi.generateText(request)
                    textResponse.response
                }

                return Result.success(response)
            } catch (e: Exception) {
                if (attempts >= maxAttempts) {
                    return Result.failure(e)
                }
                // Ждем перед повторной попыткой
                kotlinx.coroutines.delay(1000L * attempts) // 1s, 2s, 3s
            }
        }
        
        return Result.failure(Exception("Failed after $maxAttempts attempts"))
    }
}
