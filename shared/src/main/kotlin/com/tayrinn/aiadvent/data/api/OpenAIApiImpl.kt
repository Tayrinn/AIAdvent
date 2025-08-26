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
import java.time.Duration
import java.net.URL
import java.net.HttpURLConnection

class OpenAIApiImplInternal : OpenAIApi {
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true  // Важно! Включаем сериализацию значений по умолчанию
    }
    
    // Конфигурация OpenAI API
    private val configService = ConfigService()
    private var apiKey: String = ""
    private var defaultModel: String = "gpt-5"
    private var maxTokens: Int = 2000
    private var isGpt5: Boolean = true

    
    // Проверяем формат API ключа
    init {
        configService.loadConfig()
        apiKey = configService.getProperty("openai.api.key")
        defaultModel = configService.getProperty("openai.api.model", "gpt-5")
        maxTokens = configService.getIntProperty("openai.api.max_tokens", 2000)
        isGpt5 = defaultModel.startsWith("gpt-5")

        
        println("🔑 OpenAI API Key format check:")
        println("   Length: ${apiKey.length}")
        println("   Starts with: ${apiKey.take(7)}")
        println("   Contains 'sk-proj': ${apiKey.contains("sk-proj")}")
        
        // Тестируем сериализацию
        println("🧪 Testing serialization:")
        val testRequest = OpenAIRequest(
            model = defaultModel,
            messages = listOf(OpenAIMessage("user", "test")),
            maxCompletionTokens = maxTokens
        )
        val testJson = json.encodeToString(testRequest)
        println("   Test JSON: $testJson")
    }
    private val baseUrl = "https://api.openai.com/v1"
    
        override suspend fun chatCompletion(request: OpenAIRequest): OpenAIResponse = withContext(Dispatchers.IO) {
        try {
            // Проверяем доступность API
            println("🔍 Проверяем доступность OpenAI API...")
            try {
                val testRequest = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/models"))
                    .header("Authorization", "Bearer $apiKey")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build()
                
                val testResponse = httpClient.send(testRequest, HttpResponse.BodyHandlers.ofString())
                if (testResponse.statusCode() == 200) {
                    println("✅ OpenAI API доступен")
                } else {
                    println("⚠️ OpenAI API отвечает с кодом: ${testResponse.statusCode()}")
                }
            } catch (e: Exception) {
                println("⚠️ Не удалось проверить доступность API: ${e.message}")
            }
            
            // Детальное логирование объекта запроса
            println("🔍 OpenAI Request Object:")
            println("   Model: ${request.model}")
            println("   Messages count: ${request.messages.size}")
            println("   Max completion tokens: ${request.maxCompletionTokens}")
            
            val requestBody = json.encodeToString(request)
            
            // Проверяем размер запроса
            val requestSize = requestBody.length
            println("📏 Размер запроса: $requestSize символов")
            if (requestSize > 10000) {
                println("⚠️ Запрос очень большой (>10KB), это может вызывать проблемы")
            }
            
            // Детальное логирование для диагностики
            println("🔍 OpenAI API Request:")
            println("   URL: $baseUrl/chat/completions")
            println("   Headers: Content-Type=application/json, Authorization=Bearer ${apiKey.take(10)}...")
            println("   Request Body: $requestBody")
            
            // Пробуем через HttpURLConnection вместо HttpClient
            println("🔄 Используем HttpURLConnection...")
            
            val url = URL("$baseUrl/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("User-Agent", "AIAdvent/1.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.doOutput = true
            
            // Отправляем данные
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
                os.flush()
            }
            
            val responseCode = connection.responseCode
            println("🔍 OpenAI API Response:")
            println("   Status Code: $responseCode")
            
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("   Response Body: $responseBody")
                val result = json.decodeFromString<OpenAIResponse>(responseBody)
                println("✅ Успешный ответ получен")
                result
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                println("❌ OpenAI API Error ($responseCode): $errorBody")
                
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
                                content = "Извините, произошла ошибка при обращении к ChatGPT API. Код ошибки: $responseCode"
                            ),
                            finishReason = "error"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            println("❌ Exception calling OpenAI API: ${e.message}")
            println("❌ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            
            // Детальная диагностика сетевых проблем
            when (e) {
                is java.net.ConnectException -> {
                    println("🌐 Проблема с подключением к серверу")
                    println("   Проверьте интернет-соединение")
                    println("   Возможно, заблокирован доступ к api.openai.com")
                }
                is java.net.SocketTimeoutException -> {
                    println("⏰ Таймаут соединения")
                    println("   Сервер не отвечает в течение 2 минут")
                    println("   Попробуйте позже")
                }
                is java.io.IOException -> {
                    if (e.message?.contains("Connection reset") == true) {
                        println("🔄 Соединение сброшено сервером")
                        println("   Возможные причины:")
                        println("   - Нестабильное интернет-соединение")
                        println("   - Firewall/Proxy блокирует соединение")
                        println("   - Сервер OpenAI перегружен")
                        println("   - Проблемы с API ключом")
                    } else {
                        println("📡 Ошибка ввода-вывода: ${e.message}")
                    }
                }
                else -> {
                    println("❓ Неизвестная ошибка: ${e.javaClass.simpleName}")
                }
            }
            
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
        conversationHistory: List<ChatMessage>,
        maxTokensParam: Int?
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
                maxCompletionTokens = maxTokensParam ?: maxTokens
            )
            
            // Дополнительное логирование запроса
            println("🔍 OpenAI Request Details:")
            println("   Model: ${request.model}")
            println("   Messages count: ${request.messages.size}")
            println("   Max completion tokens: ${request.maxCompletionTokens}")
            println("   Is GPT-5: $isGpt5")
            println("   Max tokens param: $maxTokensParam")
            println("   Default max tokens: $maxTokens")
            
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
