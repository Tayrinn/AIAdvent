package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

actual class OpenAIApiImpl : OpenAIApi {
    
    private val httpClient = HttpClient.newBuilder().build()
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    // OpenAI API key - загружается из переменных окружения
    private val apiKey = System.getenv("OPENAI_API_KEY")
    private val baseUrl = "https://api.openai.com/v1"
    
    override suspend fun chatCompletion(request: OpenAIRequest): OpenAIResponse = withContext(Dispatchers.IO) {
        try {
            val requestBody = json.encodeToString(request)
            
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                json.decodeFromString<OpenAIResponse>(response.body())
            } else {
                // Handle error response
                val errorBody = response.body()
                println("OpenAI API Error (${response.statusCode()}): $errorBody")
                
                // Return error as response
                OpenAIResponse(
                    id = "error",
                    `object` = "chat.completion",
                    created = System.currentTimeMillis() / 1000,
                    model = request.model,
                    choices = listOf(
                        OpenAIChoice(
                            index = 0,
                            message = OpenAIMessage(
                                role = "assistant",
                                content = "Извините, произошла ошибка при обращении к ChatGPT API. Код ошибки: ${response.statusCode()}"
                            ),
                            finishReason = "error"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            println("Exception calling OpenAI API: ${e.message}")
            e.printStackTrace()
            
            // Return error response
            OpenAIResponse(
                id = "error",
                `object` = "chat.completion",
                created = System.currentTimeMillis() / 1000,
                model = request.model,
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        message = OpenAIMessage(
                            role = "assistant",
                            content = "Произошла ошибка при подключении к ChatGPT: ${e.message}"
                        ),
                        finishReason = "error"
                    )
                )
            )
        }
    }
    
    override suspend fun sendMessage(
        message: String, 
        conversationHistory: List<ChatMessage>
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            // Создаем контекст из истории разговора
            val messages = mutableListOf<OpenAIMessage>()
            
            // Добавляем системное сообщение
            messages.add(OpenAIMessage(
                role = "system",
                content = "Ты - полезный AI помощник. Отвечай на русском языке, будь дружелюбным и информативным."
            ))
            
            // Добавляем последние сообщения из истории для контекста (последние 10)
            conversationHistory.takeLast(10).forEach { chatMessage ->
                if (chatMessage.isUser) {
                    messages.add(OpenAIMessage(role = "user", content = chatMessage.content))
                } else if (!chatMessage.isError && !chatMessage.isTestReport) {
                    messages.add(OpenAIMessage(role = "assistant", content = chatMessage.content))
                }
            }
            
            // Добавляем текущее сообщение пользователя
            messages.add(OpenAIMessage(role = "user", content = message))
            
            // Отправляем запрос к OpenAI
            val request = OpenAIRequest(
                model = "gpt-5",
                messages = messages,
                maxTokens = 1000,
                temperature = 0.7
            )
            
            val response = chatCompletion(request)
            
            val assistantResponse = response.choices.firstOrNull()?.message?.content 
                ?: "Извините, не удалось получить ответ от ChatGPT"
            
            // Возвращаем два ответа для совместимости с существующим интерфейсом
            // Agent 1 - основной ответ от ChatGPT
            // Agent 2 - краткое дополнение
            val agent1Response = assistantResponse
            val agent2Response = "✨ Ответ сгенерирован ChatGPT"
            
            Pair(agent1Response, agent2Response)
            
        } catch (e: Exception) {
            println("Error in sendMessage: ${e.message}")
            e.printStackTrace()
            Pair(
                "Произошла ошибка при обращении к ChatGPT: ${e.message}",
                "Проверьте подключение к интернету и API ключ"
            )
        }
    }
}
