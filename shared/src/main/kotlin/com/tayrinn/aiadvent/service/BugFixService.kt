package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.model.Bug
import com.tayrinn.aiadvent.data.model.BugAnalysis
import com.tayrinn.aiadvent.data.model.Fix
import com.tayrinn.aiadvent.data.repository.OpenAIChatRepository

class BugFixService(
    private val openAIRepository: OpenAIChatRepository
) {
    // BugFixService использует модель из OpenAIChatRepository
    // которая уже настроена в OpenAIApiImpl

    suspend fun analyzeAndFixBugs(sourceCode: String): BugAnalysis {
        return try {
            println("🔍 Анализирую код на наличие багов через ИИ...")
            
            // Анализируем код через ИИ
            val bugs = analyzeCodeWithAI(sourceCode)
            
            // Создаем исправления на основе найденных багов
            val fixes = createFixesFromBugs(bugs)
            
            // Формируем итоговый отчет
            val summary = if (bugs.isEmpty()) {
                "✅ Код проанализирован, явных проблем не найдено"
            } else {
                "🔍 Найдено ${bugs.size} проблем, предложено ${fixes.size} исправлений"
            }
            
            BugAnalysis(bugs, fixes, summary)
        } catch (e: Exception) {
            println("❌ Ошибка при анализе багов: ${e.message}")
            BugAnalysis(
                bugs = listOf(Bug(0, "analysis_error", "Ошибка при анализе кода: ${e.message}", "high")),
                fixes = emptyList(),
                summary = "❌ Ошибка при анализе результатов"
            )
        }
    }
    
    private suspend fun analyzeCodeWithAI(sourceCode: String): List<Bug> {
        val prompt = """
            Проанализируй код на Kotlin и найди баги. Код:
            ```kotlin
            $sourceCode
            ```
            
            Ищи: неиспользуемые переменные, опасные операторы !!, логические ошибки, проблемы с null, исключения.
            
            Верни JSON:
            {
                "bugs": [
                    {
                        "line": номер_строки,
                        "type": "тип_проблемы", 
                        "description": "описание",
                        "severity": "high|medium|low"
                    }
                ]
            }
        """.trimIndent()

        return try {
            // Используем прямую отправку в OpenAI API для анализа багов
            // Передаем maxTokens для корректной работы с GPT-5
            val response = openAIRepository.sendMessage(prompt, emptyList(), 4000)
            val bugs = parseAIResponse(response.first)
            
            // Выводим детали анализа сразу
            println("🤖 **AI АНАЛИЗ ЗАВЕРШЕН:**")
            if (bugs.isNotEmpty()) {
                println("🔍 Найдено ${bugs.size} проблем:")
                bugs.forEach { bug ->
                    val severityIcon = when (bug.severity) {
                        "high" -> "🔴"
                        "medium" -> "🟡"
                        "low" -> "🟢"
                        else -> "⚪"
                    }
                    println("$severityIcon Строка ${bug.line}: ${bug.type} - ${bug.description}")
                }
            } else {
                println("✅ Проблем не найдено")
            }
            println()
            
            bugs
        } catch (e: Exception) {
            println("❌ Ошибка AI анализа: ${e.message}")
            emptyList()
        }
    }

    private fun parseAIResponse(response: String): List<Bug> {
        return try {
            // Простой парсинг JSON ответа
            if (response.contains("\"bugs\"")) {
                val bugs = mutableListOf<Bug>()
                val bugMatches = Regex("\"line\":\\s*(\\d+).*?\"type\":\\s*\"([^\"]+)\".*?\"description\":\\s*\"([^\"]+)\".*?\"severity\":\\s*\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL).findAll(response)
                
                for (match in bugMatches) {
                    val line = match.groupValues[1].toIntOrNull() ?: 1
                    val type = match.groupValues[2]
                    val description = match.groupValues[3]
                    val severity = match.groupValues[4]
                    
                    bugs.add(Bug(line, type, description, severity))
                }
                bugs
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Ошибка парсинга AI ответа: ${e.message}")
            emptyList()
        }
    }

    fun createFixesFromBugs(bugs: List<Bug>): List<Fix> {
        val fixes = mutableListOf<Fix>()
        for (bug in bugs) {
            val fix = when (bug.type) {
                "unused_variable" -> "// Удалить неиспользуемую переменную или использовать её"
                "unsafe_operator" -> "// Заменить !! на безопасную проверку на null"
                "logical_error" -> "// Исправить логику операции"
                "string_handling" -> "// Добавить обработку строки (например, trim())"
                "infinite_loop" -> "// Добавить инкремент счетчика в цикл"
                "null_safety" -> "// Добавить проверку на null перед использованием"
                "unused_import" -> "// Удалить неиспользуемый импорт"
                "exception_risk" -> "// Добавить проверку на null перед использованием"
                "inefficient_code" -> "// Оптимизировать алгоритм или структуру данных"
                else -> "// Проверить и исправить логику"
            }

            val explanation = when (bug.type) {
                "unused_variable" -> "Неиспользуемые переменные занимают память и усложняют код"
                "unsafe_operator" -> "Оператор !! может вызвать NullPointerException при runtime"
                "logical_error" -> "Логическая ошибка может привести к неправильным результатам"
                "string_handling" -> "Строки часто содержат лишние пробелы, которые нужно убирать"
                "infinite_loop" -> "Бесконечный цикл заблокирует выполнение программы"
                "null_safety" -> "Null safety - важная концепция в Kotlin для предотвращения ошибок"
                "unused_import" -> "Неиспользуемые импорты загромождают код"
                "exception_risk" -> "Потенциальные исключения могут привести к краху приложения"
                "inefficient_code" -> "Неэффективный код занимает больше ресурсов и времени"
                else -> "Проблема требует внимания разработчика"
            }

            fixes.add(Fix(bug.line, fix, explanation))
        }
        return fixes
    }

    /**
     * Генерирует исправленный код с помощью AI
     */
    suspend fun generateFixedCode(sourceCode: String, bugs: List<Bug>): String {
        if (bugs.isEmpty()) {
            return sourceCode
        }

        val prompt = """
            Исправь код на Kotlin, устранив найденные проблемы. 
            
            Исходный код:
            ```kotlin
            $sourceCode
            ```
            
            Найденные проблемы:
            ${bugs.joinToString("\n") { "- Строка ${it.line}: ${it.type} - ${it.description}" }}
            
            Верни только исправленный код без комментариев и объяснений.
        """.trimIndent()

        return try {
            val response = openAIRepository.sendMessage(prompt, emptyList(), 4000)
            response.first
        } catch (e: Exception) {
            println("❌ Ошибка генерации исправленного кода: ${e.message}")
            sourceCode // Возвращаем исходный код в случае ошибки
        }
    }
}
