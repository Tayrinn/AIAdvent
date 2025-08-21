package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.OpenAIApi
import com.tayrinn.aiadvent.data.model.OpenAIMessage
import com.tayrinn.aiadvent.data.model.OpenAIRequest

/**
 * Сервис для генерации тестов через ChatGPT
 */
class TestGenerationService(
    private val openAIApi: OpenAIApi
) {
    
    /**
     * Генерирует тесты для исходного кода
     */
    suspend fun generateTests(sourceCode: String, fileName: String): String {
        val prompt = createTestGenerationPrompt(sourceCode, fileName)
        
        val request = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = """Ты - эксперт по тестированию программного кода. 
Твоя задача - создавать качественные unit тесты для предоставленного кода.

Правила:
1. Создавай тесты на том же языке, что и исходный код
2. Используй стандартные фреймворки тестирования (JUnit для Java/Kotlin, pytest для Python, etc.)
3. Покрывай все публичные методы и функции
4. Включай позитивные и негативные тесты
5. Добавляй комментарии к сложным тестам
6. Используй понятные имена для тестов
7. Группируй связанные тесты в классы/функции
8. Возвращай только код тестов без дополнительных пояснений"""
                ),
                OpenAIMessage(
                    role = "user",
                    content = prompt
                )
            ),
            maxTokens = 2000,
            temperature = 0.3
        )
        
        return try {
            val response = openAIApi.chatCompletion(request)
            response.choices.firstOrNull()?.message?.content 
                ?: "Ошибка: не удалось получить ответ от ChatGPT"
        } catch (e: Exception) {
            "Ошибка генерации тестов: ${e.message}"
        }
    }
    
    /**
     * Создает промпт для генерации тестов
     */
    private fun createTestGenerationPrompt(sourceCode: String, fileName: String): String {
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val language = when (extension) {
            "kt" -> "Kotlin"
            "java" -> "Java"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "cpp", "c" -> "C++"
            "cs" -> "C#"
            "go" -> "Go"
            "rs" -> "Rust"
            else -> "неизвестный язык"
        }
        
        return """Создай unit тесты для следующего кода на языке $language.

Имя файла: $fileName

Исходный код:
$sourceCode

Создай полные unit тесты, которые покрывают все функции и методы. 
Используй стандартные фреймворки тестирования для $language.
Возвращай только код тестов без дополнительных пояснений."""
    }
    
    /**
     * Генерирует промпт для анализа результатов тестов
     */
    suspend fun analyzeTestResults(testOutput: String, originalCode: String): String {
        val prompt = """Проанализируй результаты выполнения тестов и создай подробный отчет.

Результаты тестов:
$testOutput

Исходный код:
$originalCode

Создай отчет, который включает:
1. Общее количество тестов
2. Количество пройденных/проваленных тестов
3. Анализ ошибок (если есть)
4. Рекомендации по улучшению кода
5. Общую оценку качества тестирования

Отвечай на русском языке."""

        val request = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = "Ты - эксперт по анализу результатов тестирования. Создавай подробные и понятные отчеты."
                ),
                OpenAIMessage(
                    role = "user",
                    content = prompt
                )
            ),
            maxTokens = 1500,
            temperature = 0.2
        )
        
        return try {
            val response = openAIApi.chatCompletion(request)
            response.choices.firstOrNull()?.message?.content 
                ?: "Ошибка: не удалось получить анализ от ChatGPT"
        } catch (e: Exception) {
            "Ошибка анализа результатов: ${e.message}"
        }
    }
}
