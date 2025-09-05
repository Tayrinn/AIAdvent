package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.createOpenAIApiImpl
import com.tayrinn.aiadvent.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesExtractionService {
    
    private val openAIApi = createOpenAIApiImpl()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Извлекает предпочтения пользователя из последних сообщений диалога
     */
    suspend fun extractPreferences(
        messages: List<ChatMessage>,
        currentPreferences: UserPreferences?,
        userId: String
    ): UserPreferences? = withContext(Dispatchers.IO) {
        try {
            // Берем только последние 10 сообщений для анализа
            val recentMessages = messages.takeLast(10)
            
            // Создаем промпт для извлечения предпочтений
            val extractionPrompt = createExtractionPrompt(recentMessages, currentPreferences)
            
            // Отправляем запрос к ИИ
            val (extractedText, _) = openAIApi.sendMessage(
                message = "$extractionPrompt\n\nПроанализируй диалог и извлеки предпочтения пользователя в формате JSON.",
                recentMessages = emptyList(), // Не используем контекст для извлечения предпочтений
                modelName = "deepseek-ai/DeepSeek-V3-0324"
            )
            
            println("🧠 ИИ ответ для извлечения предпочтений: $extractedText")
            
            // Пытаемся найти JSON в ответе
            val jsonMatch = Regex("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}").find(extractedText)
            val jsonString = jsonMatch?.value ?: return@withContext null
            
            println("📋 Найденный JSON: $jsonString")
            
            // Парсим JSON в объект предпочтений
            val extractedPrefs = try {
                json.decodeFromString<UserPreferences>(jsonString)
            } catch (e: Exception) {
                println("❌ Ошибка парсинга JSON предпочтений: ${e.message}")
                return@withContext null
            }
            
            // Обновляем предпочтения с правильным userId
            val updatedPrefs = extractedPrefs.copy(
                userId = userId,
                lastUpdated = System.currentTimeMillis()
            )
            
            println("✅ Извлеченные предпочтения: $updatedPrefs")
            updatedPrefs
            
        } catch (e: Exception) {
            println("❌ Ошибка извлечения предпочтений: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private fun createExtractionPrompt(
        messages: List<ChatMessage>,
        currentPreferences: UserPreferences?
    ): String {
        val dialogText = messages.joinToString("\n") { message ->
            val role = if (message.isUser) "user" else "assistant"
            "$role: ${message.content}"
        }
        
        val currentPrefsText = currentPreferences?.let { 
            json.encodeToString(it)
        } ?: "null"
        
        return """
Ты - эксперт по анализу диалогов. Твоя задача - извлечь предпочтения пользователя из диалога.

ДИАЛОГ:
$dialogText

ТЕКУЩИЕ ПРЕДПОЧТЕНИЯ (если есть):
$currentPrefsText

ИНСТРУКЦИИ:
1. Проанализируй диалог и найди информацию о предпочтениях пользователя
2. Обрати внимание на:
   - Язык общения (ru, en, etc.)
   - Имя пользователя (если упоминается)
   - Стиль общения (formal, friendly, casual)
   - Предпочитаемую длину ответов (short, medium, long)
   - Интересы и хобби
   - Области экспертизы
   - Предпочитаемые темы
   - Темы, которых следует избегать

3. Если текущие предпочтения уже есть, обнови только те поля, для которых найдена новая информация
4. Если информации недостаточно для определения предпочтения, оставь значение по умолчанию
5. Ответь ТОЛЬКО JSON объектом в следующем формате:

{
  "userId": "user_id_placeholder",
  "language": "ru",
  "name": null,
  "communicationStyle": "friendly",
  "responseLength": "medium",
  "interests": [],
  "expertise": [],
  "timezone": null,
  "preferredTopics": [],
  "avoidTopics": [],
  "lastUpdated": ${System.currentTimeMillis()}
}

ВАЖНО: Отвечай ТОЛЬКО JSON, без дополнительных комментариев или объяснений!
        """.trimIndent()
    }
}
