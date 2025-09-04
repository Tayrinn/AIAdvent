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

/**
 * Исключение, которое бросается, когда модель не смогла справиться с задачей
 */
class AIModelFailureException(message: String) : Exception(message)

class OpenAIApiImplInternal : OpenAIApi {

    private val httpClient = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true  // Важно! Включаем сериализацию значений по умолчанию
    }

    // Конфигурация Hugging Face API
    private val configService = ConfigService
    private var apiKey: String = ""
    private var defaultModel: String = "deepseek-ai/DeepSeek-V3-0324"
    private var maxTokens: Int = 2000
    private var temperature: Double = 0.7


    // Проверяем формат API ключа
    init {
        configService.loadConfig()
        apiKey = configService.getProperty("huggingface.api.key")
        defaultModel = configService.getProperty("huggingface.api.model", "deepseek-ai/DeepSeek-V3-0324")
        maxTokens = configService.getIntProperty("huggingface.api.max_tokens", 2000)
        temperature = configService.getDoubleProperty("huggingface.api.temperature", 0.7)


        println("🔑 Hugging Face API Key format check:")
        println("   Length: ${apiKey.length}")
        println("   Starts with: ${apiKey.take(7)}")
        println("   Contains 'hf_': ${apiKey.contains("hf_")}")

        // Тестируем сериализацию
        println("🧪 Testing serialization:")
        val testRequest = HuggingFaceRequest(
            model = defaultModel,
            messages = listOf(HuggingFaceMessage("user", "test"))
        )
        val testJson = json.encodeToString(testRequest)
        println("   Test JSON: $testJson")
    }
    private val baseUrl = "https://router.huggingface.co"
    
        override suspend fun chatCompletion(request: OpenAIRequest): OpenAIResponse = withContext(Dispatchers.IO) {
        try {
            // Конвертируем OpenAI запрос в Hugging Face формат
            val hfMessages = request.messages.map { message ->
                HuggingFaceMessage(
                    role = message.role,
                    content = message.content
                )
            }

            val hfRequest = HuggingFaceRequest(
                model = request.model.ifEmpty { defaultModel },
                messages = hfMessages
            )

            // Детальное логирование объекта запроса
            println("🔍 Hugging Face Request Object:")
            println("   Model: ${hfRequest.model}")
            println("   Messages count: ${hfRequest.messages.size}")

            val requestBody = json.encodeToString(hfRequest)

            // Проверяем размер запроса
            val requestSize = requestBody.length
            println("📏 Размер запроса: $requestSize символов")
            if (requestSize > 10000) {
                println("⚠️ Запрос очень большой (>10KB), это может вызывать проблемы")
            }

            // Детальное логирование для диагностики
            println("🔍 Hugging Face API Request:")
            println("   URL: $baseUrl/v1/chat/completions")
            println("   Headers: Content-Type=application/json, Authorization=Bearer ${apiKey.take(10)}...")
            println("   Request Body: $requestBody")

            // Используем HttpURLConnection
            println("🔄 Используем HttpURLConnection...")

            val url = URL("$baseUrl/v1/chat/completions")
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
            println("🔍 Hugging Face API Response:")
            println("   Status Code: $responseCode")

            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("   Response Body: $responseBody")

                try {
                    val hfResult = json.decodeFromString<HuggingFaceResponse>(responseBody)

                    // Конвертируем Hugging Face ответ в OpenAI формат
                    val result = OpenAIResponse(
                        id = hfResult.id,
                        `object` = hfResult.`object`,
                        created = hfResult.created,
                        model = hfResult.model,
                        choices = hfResult.choices.map { choice ->
                            OpenAIChoice(
                                index = choice.index,
                                message = OpenAIMessage(
                                    role = choice.message.role,
                                    content = choice.message.content
                                ),
                                finishReason = choice.finishReason
                            )
                        },
                        usage = hfResult.usage?.let { usage ->
                            OpenAIUsage(
                                promptTokens = usage.promptTokens,
                                completionTokens = usage.completionTokens,
                                totalTokens = usage.totalTokens
                            )
                        }
                    )

                    println("✅ Успешный ответ получен")

                    // Проверяем, что content не пустой
                    val content = result.choices.firstOrNull()?.message?.content
                    println("🔍 Проверяем content в chatCompletion: length=${content?.length ?: 0}, isNullOrBlank=${content.isNullOrBlank()}")
                    if (content.isNullOrBlank()) {
                        println("❌ Обнаружен пустой content в ответе Hugging Face API!")
                        throw AIModelFailureException("Модель AI вернула пустой ответ. Возможно, запрос слишком сложный или модель не смогла справиться с задачей.")
                    }

                    result
                } catch (e: Exception) {
                    println("❌ Ошибка парсинга ответа Hugging Face API: ${e.message}")
                    throw AIModelFailureException("Не удалось обработать ответ от Hugging Face API: ${e.message}")
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                println("❌ Hugging Face API Error ($responseCode): $errorBody")

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
                                content = "Извините, произошла ошибка при обращении к Hugging Face API. Код ошибки: $responseCode"
                            ),
                            finishReason = "error"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            println("❌ Exception calling Hugging Face API: ${e.message}")
            println("❌ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()

            // Детальная диагностика сетевых проблем
            when (e) {
                is java.net.ConnectException -> {
                    println("🌐 Проблема с подключением к серверу")
                    println("   Проверьте интернет-соединение")
                    println("   Возможно, заблокирован доступ к router.huggingface.co")
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
                        println("   - Сервер Hugging Face перегружен")
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
                            content = "Произошла ошибка при подключении к Hugging Face: ${e.message}"
                        ),
                        finishReason = "error"
                    )
                )
            )
        }
    }
    
        override suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
        modelName: String?
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            // Создаем контекст с использованием последних сообщений
            val messages = mutableListOf<OpenAIMessage>()

            // Добавляем системное сообщение
            messages.add(OpenAIMessage(
                role = "system",
                content = "Ты - полезный AI помощник. Отвечай на русском языке, будь дружелюбным и информативным."
            ))

            // Добавляем последние сообщения для контекста (максимум 3)
            val contextMessages = recentMessages.takeLast(3)
            contextMessages.forEach { chatMessage ->
                if (chatMessage.isUser) {
                    messages.add(OpenAIMessage(role = "user", content = chatMessage.content))
                } else if (!chatMessage.isError && !chatMessage.isTestReport) {
                    // Извлекаем только текст ответа AI (убираем форматирование)
                    val aiContent = chatMessage.content
                        .replace(Regex("""🤖 \*\*.*?:\*\* """), "") // Убираем форматирование AI
                        .replace(Regex("""🌡️ [\d.]+.*?:\*\* """), "") // Убираем форматирование температуры
                    messages.add(OpenAIMessage(role = "assistant", content = aiContent))
                }
            }

            // Добавляем текущее сообщение пользователя
            messages.add(OpenAIMessage(role = "user", content = message))
            
            // Отправляем запрос к Hugging Face
            val request = OpenAIRequest(
                model = modelName ?: defaultModel,
                messages = messages,
                maxCompletionTokens = maxTokens
            )
            
            // Дополнительное логирование запроса
            println("🔍 Hugging Face Request Details:")
            println("   Model: ${request.model}")
            println("   Messages count: ${request.messages.size}")
            
            val response = chatCompletion(request)

            val assistantResponse = response.choices.firstOrNull()?.message?.content

            // Проверяем, что ответ не пустой
            println("🔍 Проверяем ответ от AI: length=${assistantResponse?.length ?: 0}, isNullOrBlank=${assistantResponse.isNullOrBlank()}")
            if (assistantResponse.isNullOrBlank()) {
                println("❌ Обнаружен пустой ответ от AI модели!")
                throw AIModelFailureException("Модель AI не смогла справиться с задачей и вернула пустой ответ. Возможно, запрос слишком сложный или модель перегружена.")
            }

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
