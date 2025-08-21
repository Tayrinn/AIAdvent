package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.*
import com.tayrinn.aiadvent.util.ConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenAIApiImplInternal : OpenAIApi {
    
    private val httpClient = HttpClient.newBuilder().build()
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true  // Важно! Включаем сериализацию значений по умолчанию
    }
    
    // Конфигурация OpenAI API
    private val configService = ConfigService()
    private val apiKey: String
    private val defaultModel: String
    private val maxTokens: Int
    private val temperature: Double
    
    // Проверяем формат API ключа
    init {
        configService.loadConfig()
        apiKey = configService.getProperty("openai.api.key")
        defaultModel = configService.getProperty("openai.api.model", "gpt-3.5-turbo")
        maxTokens = configService.getIntProperty("openai.api.max_tokens", 2000)
        temperature = configService.getDoubleProperty("openai.api.temperature", 0.7)
        
        println("🔑 OpenAI API Key format check:")
        println("   Length: ${apiKey.length}")
        println("   Starts with: ${apiKey.take(7)}")
        println("   Contains 'sk-proj': ${apiKey.contains("sk-proj")}")
        
        // Тестируем сериализацию
        println("🧪 Testing serialization:")
        val testRequest = OpenAIRequest(
            model = defaultModel,
            messages = listOf(OpenAIMessage("user", "test")),
            maxTokens = maxTokens,
            temperature = temperature
        )
        val testJson = json.encodeToString(testRequest)
        println("   Test JSON: $testJson")
    }
    private val baseUrl = "https://api.openai.com/v1"
    
    override suspend fun chatCompletion(request: OpenAIRequest): OpenAIResponse = withContext(Dispatchers.IO) {
        try {
            // Детальное логирование объекта запроса
            println("🔍 OpenAI Request Object:")
            println("   Model: ${request.model}")
            println("   Messages count: ${request.messages.size}")
            println("   Max tokens: ${request.maxTokens}")
            println("   Temperature: ${request.temperature}")
            
            val requestBody = json.encodeToString(request)
            
            // Детальное логирование для диагностики
            println("🔍 OpenAI API Request:")
            println("   URL: $baseUrl/chat/completions")
            println("   Headers: Content-Type=application/json, Authorization=Bearer ${apiKey.take(10)}...")
            println("   Request Body: $requestBody")
            
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            
            // Детальное логирование ответа
            println("🔍 OpenAI API Response:")
            println("   Status Code: ${response.statusCode()}")
            println("   Response Headers: ${response.headers()}")
            println("   Response Body: ${response.body()}")
            
            if (response.statusCode() == 200) {
                json.decodeFromString<OpenAIResponse>(response.body())
            } else {
                // Handle error response
                val errorBody = response.body()
                println("❌ OpenAI API Error (${response.statusCode()}): $errorBody")
                
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
                model = defaultModel,
                messages = messages,
                maxTokens = maxTokens,
                temperature = temperature
            )
            
            // Дополнительное логирование запроса
            println("🔍 OpenAI Request Details:")
            println("   Model: ${request.model}")
            println("   Messages count: ${request.messages.size}")
            println("   Max tokens: ${request.maxTokens}")
            println("   Temperature: ${request.temperature}")
            
            val response = chatCompletion(request)
            
            val assistantResponse = response.choices.firstOrNull()?.message?.content 
                ?: "Извините, не удалось получить ответ от ChatGPT"
            
            // Возвращаем только один ответ от ChatGPT
            Pair(assistantResponse, "")
            
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
