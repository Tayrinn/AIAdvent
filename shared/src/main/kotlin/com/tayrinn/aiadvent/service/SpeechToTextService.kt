package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.HuggingFaceApi
import com.tayrinn.aiadvent.data.api.createHuggingFaceApi
import com.tayrinn.aiadvent.data.model.WhisperResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.sound.sampled.*

/**
 * Сервис для распознавания речи с помощью Hugging Face Whisper модели
 */
class SpeechToTextService(
    private val huggingFaceApi: HuggingFaceApi = createHuggingFaceApi()
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Записывает аудио с микрофона в течение указанного времени
     */
    suspend fun recordAudio(durationSeconds: Int = 5): ByteArray = withContext(Dispatchers.IO) {
        val format = AudioFormat(16000f, 16, 1, true, false) // 16kHz, 16-bit, mono
        val info = DataLine.Info(TargetDataLine::class.java, format)

        if (!AudioSystem.isLineSupported(info)) {
            throw Exception("Микрофон не поддерживается")
        }

        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()

        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        val outputStream = ByteArrayOutputStream()

        println("🎤 Начинаем запись аудио ($durationSeconds сек)...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
            val bytesRead = line.read(buffer, 0, bufferSize)
            outputStream.write(buffer, 0, bytesRead)
        }

        line.stop()
        line.close()

        val audioData = outputStream.toByteArray()
        println("✅ Запись завершена, размер: ${audioData.size} байт")

        audioData
    }

    /**
     * Конвертирует аудио данные в WAV формат
     */
    private fun convertToWav(pcmData: ByteArray, sampleRate: Int = 16000): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // WAV header
        outputStream.write("RIFF".toByteArray())
        val fileSize = 36 + pcmData.size
        outputStream.write(byteArrayOf(
            (fileSize and 0xFF).toByte(),
            ((fileSize shr 8) and 0xFF).toByte(),
            ((fileSize shr 16) and 0xFF).toByte(),
            ((fileSize shr 24) and 0xFF).toByte()
        ))
        outputStream.write("WAVE".toByteArray())
        outputStream.write("fmt ".toByteArray())
        outputStream.write(byteArrayOf(16, 0, 0, 0)) // Subchunk1Size
        outputStream.write(byteArrayOf(1, 0)) // AudioFormat (PCM)
        outputStream.write(byteArrayOf(1, 0)) // NumChannels (mono)
        outputStream.write(byteArrayOf(
            (sampleRate and 0xFF).toByte(),
            ((sampleRate shr 8) and 0xFF).toByte(),
            ((sampleRate shr 16) and 0xFF).toByte(),
            ((sampleRate shr 24) and 0xFF).toByte()
        ))
        val byteRate = sampleRate * 2 // SampleRate * NumChannels * BitsPerSample/8
        outputStream.write(byteArrayOf(
            (byteRate and 0xFF).toByte(),
            ((byteRate shr 8) and 0xFF).toByte(),
            ((byteRate shr 16) and 0xFF).toByte(),
            ((byteRate shr 24) and 0xFF).toByte()
        ))
        outputStream.write(byteArrayOf(2, 0)) // BlockAlign
        outputStream.write(byteArrayOf(16, 0)) // BitsPerSample
        outputStream.write("data".toByteArray())
        outputStream.write(byteArrayOf(
            (pcmData.size and 0xFF).toByte(),
            ((pcmData.size shr 8) and 0xFF).toByte(),
            ((pcmData.size shr 16) and 0xFF).toByte(),
            ((pcmData.size shr 24) and 0xFF).toByte()
        ))
        outputStream.write(pcmData)

        return outputStream.toByteArray()
    }

    /**
     * Отправляет аудио на распознавание речи через Hugging Face Whisper
     */
    suspend fun transcribeAudio(audioData: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            println("🎯 Отправляем аудио на распознавание...")

            val wavData = convertToWav(audioData)

            val response = huggingFaceApi.transcribeAudio(wavData)

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val whisperResponse = json.decodeFromString<WhisperResponse>(responseBody)
                    val text = whisperResponse.text.trim()
                    println("✅ Распознано: \"$text\"")
                    return@withContext text
                } else {
                    throw Exception("Пустой ответ от API")
                }
            } else {
                throw Exception("Ошибка API: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            println("❌ Ошибка распознавания речи: ${e.message}")
            throw e
        }
    }

    /**
     * Полная функция записи и распознавания речи
     */
    suspend fun recordAndTranscribe(durationSeconds: Int = 5): String {
        val audioData = recordAudio(durationSeconds)
        return transcribeAudio(audioData)
    }

    /**
     * Генерирует речь из текста с использованием Python скрипта и gTTS
     */
    suspend fun generateSpeech(text: String): String = withContext(Dispatchers.IO) {
        try {
            println("🔊 Запускаем генерацию речи через Python...")

            // Определяем пути
            val projectDir = System.getProperty("user.dir")
            val scriptPath = "$projectDir/desktop/src/main/python/tts_service.py"
            val venvPython = "$projectDir/venv_tts/bin/python3"

            // Исправляем путь, если мы находимся в desktop директории
            val actualProjectDir = if (projectDir.endsWith("/desktop")) {
                projectDir.substring(0, projectDir.length - 8) // Убираем "/desktop"
            } else {
                projectDir
            }

            val actualScriptPath = "$actualProjectDir/desktop/src/main/python/tts_service.py"
            val actualVenvPython = "$actualProjectDir/venv_tts/bin/python3"

            println("📂 Путь к скрипту: $actualScriptPath")
            println("🐍 Python из venv: $actualVenvPython")

            // Проверяем существование файлов
            val scriptFile = java.io.File(actualScriptPath)
            val pythonFile = java.io.File(actualVenvPython)

            if (!scriptFile.exists()) {
                throw Exception("Python скрипт не найден: $actualScriptPath")
            }

            if (!pythonFile.exists()) {
                throw Exception("Python из виртуального окружения не найден: $actualVenvPython")
            }

            // Создаем процесс Python с виртуальным окружением
            val process = ProcessBuilder(actualVenvPython, actualScriptPath, text)
                .redirectErrorStream(true)
                .start()

            // Читаем вывод процесса
            val reader = process.inputStream.bufferedReader()
            var audioFilePath: String? = null
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                println("Python: $line")
                if (line?.startsWith("/") == true || line?.contains("\\") == true) {
                    audioFilePath = line
                }
            }

            // Ждем завершения процесса
            val exitCode = process.waitFor()

            if (exitCode == 0 && audioFilePath != null) {
                println("✅ Python скрипт выполнен успешно")

                // Проверяем, что файл существует
                val audioFile = java.io.File(audioFilePath)
                if (audioFile.exists() && audioFile.length() > 0) {
                    println("✅ Аудио файл создан успешно, размер: ${audioFile.length()} байт")
                    audioFilePath
                } else {
                    throw Exception("Аудио файл не найден или пустой: $audioFilePath")
                }
            } else {
                throw Exception("Ошибка выполнения Python скрипта (код выхода: $exitCode)")
            }
        } catch (e: Exception) {
            println("❌ Ошибка генерации речи: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
