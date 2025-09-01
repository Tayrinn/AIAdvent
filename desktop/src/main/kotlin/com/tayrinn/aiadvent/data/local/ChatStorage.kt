package com.tayrinn.aiadvent.data.local

import com.tayrinn.aiadvent.data.model.ChatMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Простое файловое хранилище для сообщений чата на desktop
 */
class ChatStorage(private val storageDir: String = "chat_data") {

    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageFile: File
        get() = File(storageDir, "messages.json").apply {
            parentFile?.mkdirs()
        }

    init {
        // Создаем директорию для хранения
        File(storageDir).mkdirs()
    }

    /**
     * Сохраняет сообщение в хранилище
     */
    suspend fun saveMessage(message: ChatMessage) = mutex.withLock {
        try {
            println("💾 Начинаем сохранение сообщения: ${message.content.take(50)}...")
            val messages = loadMessagesInternal().toMutableList()
            println("📊 Загружено ${messages.size} существующих сообщений")

            // Генерируем новый ID если его нет
            val messageWithId = if (message.id == 0L) {
                val nextId = (messages.maxOfOrNull { it.id } ?: 0L) + 1
                message.copy(id = nextId)
            } else {
                message
            }

            messages.add(messageWithId)
            println("➕ Добавлено сообщение с ID: ${messageWithId.id}")

            // Ограничиваем количество сообщений (оставляем последние 100)
            if (messages.size > 100) {
                messages.sortBy { it.timestamp }
                messages.takeLast(100)
            }

            saveMessagesInternal(messages)
            println("✅ Сообщение сохранено в файл: ${storageFile.absolutePath}")
            messageWithId
        } catch (e: Exception) {
            println("❌ Ошибка сохранения сообщения: ${e.message}")
            e.printStackTrace()
            message // Возвращаем оригинальное сообщение в случае ошибки
        }
    }

    /**
     * Загружает все сообщения
     */
    suspend fun loadMessages(): List<ChatMessage> = mutex.withLock {
        loadMessagesInternal()
    }

    /**
     * Получает последние N сообщений
     */
    suspend fun getLastMessages(limit: Int): List<ChatMessage> = mutex.withLock {
        loadMessagesInternal()
            .sortedBy { it.timestamp }
            .takeLast(limit)
    }

    /**
     * Очищает все сообщения
     */
    suspend fun clearMessages() = mutex.withLock {
        try {
            if (storageFile.exists()) {
                storageFile.delete()
            }
        } catch (e: Exception) {
            println("❌ Ошибка очистки сообщений: ${e.message}")
        }
    }

    /**
     * Внутренний метод загрузки сообщений
     */
    private fun loadMessagesInternal(): List<ChatMessage> {
        return try {
            if (!storageFile.exists()) {
                emptyList()
            } else {
                val jsonContent = storageFile.readText()
                if (jsonContent.isBlank()) {
                    emptyList()
                } else {
                    json.decodeFromString(jsonContent)
                }
            }
        } catch (e: Exception) {
            println("❌ Ошибка загрузки сообщений: ${e.message}")
            emptyList()
        }
    }

    /**
     * Внутренний метод сохранения сообщений
     */
    private fun saveMessagesInternal(messages: List<ChatMessage>) {
        try {
            println("💾 Сохраняем ${messages.size} сообщений в файл...")
            val jsonContent = json.encodeToString(messages)
            println("📝 Сериализован JSON размером: ${jsonContent.length} символов")
            storageFile.writeText(jsonContent)
            println("✅ Файл записан: ${storageFile.absolutePath} (${storageFile.length()} байт)")
        } catch (e: Exception) {
            println("❌ Ошибка сохранения сообщений: ${e.message}")
            e.printStackTrace()
        }
    }
}
