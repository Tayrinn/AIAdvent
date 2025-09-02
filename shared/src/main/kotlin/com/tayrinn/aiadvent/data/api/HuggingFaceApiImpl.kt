package com.tayrinn.aiadvent.data.api

import com.tayrinn.aiadvent.data.model.WhisperResponse
import com.tayrinn.aiadvent.util.ConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Hugging Face API interface
 */
interface HuggingFaceApi {
    /**
     * Transcribe audio using Whisper model
     * @param audioData Audio data as byte array
     * @return Whisper response with transcribed text
     */
    suspend fun transcribeAudio(audioData: ByteArray): okhttp3.Response

    /**
     * Generate speech from text using TTS model
     * @param text Text to convert to speech
     * @return TTS response with audio data
     */
    suspend fun generateSpeech(text: String): okhttp3.Response
}

/**
 * Hugging Face API implementation
 */
class HuggingFaceApiImpl : HuggingFaceApi {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .build()

    private val configService = ConfigService()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun transcribeAudio(audioData: ByteArray): okhttp3.Response = withContext(Dispatchers.IO) {
        val apiKey = configService.getHuggingFaceApiKey()
        val modelUrl = "https://router.huggingface.co/hf-inference/models/openai/whisper-large-v3-turbo"

        try {

            println("🎯 Отправляем аудио на Whisper API...")

            // Создаем HTTP запрос
            val request = HttpRequest.newBuilder()
                .uri(URI.create(modelUrl))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "audio/wav")
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                println("✅ Whisper API успешно обработал аудио")

                // Парсим JSON ответ
                val responseBody = response.body()
                val whisperResponse = json.decodeFromString<WhisperResponse>(responseBody)

                // Создаем успешный okhttp3.Response
                val mediaType = "application/json".toMediaType()
                val okHttpResponseBody = ResponseBody.create(mediaType, responseBody.toByteArray())

                // Создаем okhttp3.Request для Response.Builder
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(modelUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "audio/wav")
                    .post(okhttp3.RequestBody.create("audio/wav".toMediaType(), audioData))
                    .build()

                return@withContext okhttp3.Response.Builder()
                    .request(okHttpRequest)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(response.statusCode())
                    .message("OK")
                    .body(okHttpResponseBody)
                    .build()
            } else {
                println("❌ Ошибка Whisper API: ${response.statusCode()} - ${response.body()}")

                val mediaType = "application/json".toMediaType()
                val errorBody = ResponseBody.create(mediaType, "{\"error\": \"${response.body()}\"}".toByteArray())

                // Создаем okhttp3.Request для Response.Builder
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(modelUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "audio/wav")
                    .post(okhttp3.RequestBody.create("audio/wav".toMediaType(), audioData))
                    .build()

                return@withContext okhttp3.Response.Builder()
                    .request(okHttpRequest)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(response.statusCode())
                    .message(response.body())
                    .body(errorBody)
                    .build()
            }
        } catch (e: Exception) {
            println("❌ Ошибка при отправке на Whisper API: ${e.message}")
            e.printStackTrace()

            val mediaType = "application/json".toMediaType()
            val errorBody = ResponseBody.create(mediaType, "{\"error\": \"${e.message}\"}".toByteArray())

            // Создаем okhttp3.Request для Response.Builder
            val okHttpRequest = okhttp3.Request.Builder()
                .url(modelUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "audio/wav")
                .post(okhttp3.RequestBody.create("audio/wav".toMediaType(), audioData))
                .build()

            return@withContext okhttp3.Response.Builder()
                .request(okHttpRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(500)
                .message(e.message ?: "Unknown error")
                .body(errorBody)
                .build()
        }
    }

    override suspend fun generateSpeech(text: String): okhttp3.Response = withContext(Dispatchers.IO) {
        try {
            val apiKey = configService.getHuggingFaceApiKey()
            val modelUrl = "https://router.huggingface.co/fal-ai/kokoro"

            println("🔊 Генерируем речь для текста: ${text.take(50)}...")

            // Создаем JSON запрос
            val requestJson = "{\"text\":\"${text.replace("\"", "\\\"")}\"}"
            val requestBody = requestJson.toByteArray(Charsets.UTF_8)

            // Создаем HTTP запрос
            val request = HttpRequest.newBuilder()
                .uri(URI.create(modelUrl))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                println("✅ TTS API успешно сгенерировал речь")

                // Парсим JSON ответ
                val responseBody = response.body()
                val mediaType = "application/json".toMediaType()
                val okHttpResponseBody = ResponseBody.create(mediaType, responseBody.toByteArray())

                // Создаем okhttp3.Request для Response.Builder
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(modelUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()

                return@withContext okhttp3.Response.Builder()
                    .request(okHttpRequest)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(response.statusCode())
                    .message("OK")
                    .body(okHttpResponseBody)
                    .build()
            } else {
                println("❌ Ошибка TTS API: ${response.statusCode()} - ${response.body()}")

                val mediaType = "application/json".toMediaType()
                val errorBody = ResponseBody.create(mediaType, "{\"error\": \"${response.body()}\"}".toByteArray())

                // Создаем okhttp3.Request для Response.Builder
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(modelUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()

                return@withContext okhttp3.Response.Builder()
                    .request(okHttpRequest)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(response.statusCode())
                    .message(response.body())
                    .body(errorBody)
                    .build()
            }
        } catch (e: Exception) {
            println("❌ Ошибка при генерации речи: ${e.message}")
            e.printStackTrace()

            // Создаем пустой okhttp3.Request для Response.Builder
            val okHttpRequest = okhttp3.Request.Builder()
                .url("https://router.huggingface.co/fal-ai/kokoro")
                .build()

            val mediaType = "application/json".toMediaType()
            val errorBody = ResponseBody.create(mediaType, "{\"error\": \"${e.message}\"}".toByteArray())

            return@withContext okhttp3.Response.Builder()
                .request(okHttpRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(500)
                .message(e.message ?: "Unknown error")
                .body(errorBody)
                .build()
        }
    }
}

/**
 * Factory function to create Hugging Face API implementation
 */
fun createHuggingFaceApi(): HuggingFaceApi = HuggingFaceApiImpl()
