package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.model.BugAnalysis

/**
 * Сервис для управления полным workflow тестирования:
 * 1. Анализ и исправление багов
 * 2. Генерация тестов
 * 3. Выполнение тестов
 * 4. Формирование отчетов
 */
class TestWorkflowService(
    private val fileService: FileService,
    private val bugFixService: BugFixService,
    private val testGenerationService: TestGenerationService,
    private val testExecutionService: TestExecutionService
) {

    /**
     * Выполняет полный workflow тестирования для файла
     */
    suspend fun executeTestWorkflow(
        filePath: String,
        onMessage: ((String) -> Unit)? = null
    ): String {
        return try {
            println("🚀 Запускаю workflow тестирования для файла: $filePath")
            onMessage?.invoke("🚀 **НАЧИНАЮ АНАЛИЗ ФАЙЛА:** $filePath")

            // 1. Читаем файл
            val sourceCode = fileService.readFile(filePath)
            val fileName = fileService.getFileName(filePath)
            onMessage?.invoke("📄 **ЧТЕНИЕ ФАЙЛА ЗАВЕРШЕНО:** Найдено ${sourceCode.length} символов")

            // 2. Анализируем и исправляем баги
            onMessage?.invoke("🔍 **НАЧИНАЮ АНАЛИЗ БАГОВ...**")
            val bugAnalysis = bugFixService.analyzeAndFixBugs(sourceCode)

            // 3. ПОКАЗЫВАЕМ АНАЛИЗ БАГОВ ОТДЕЛЬНЫМ СООБЩЕНИЕМ
            val bugAnalysisMessage = generateBugAnalysisMessage(bugAnalysis, fileName)
            onMessage?.invoke(bugAnalysisMessage)
            
            // 4. Генерируем исправленный код
            onMessage?.invoke("🔧 **СОХРАНЯЮ ИСПРАВЛЕННЫЙ КОД...**")
            val fixedCode = bugFixService.generateFixedCode(sourceCode, bugAnalysis.bugs, onMessage)

            // 5. Записываем исправленный код в файл
            val fixedFilePath = filePath.replace(".kt", "_Fixed.kt")
            fileService.writeFile(fixedFilePath, fixedCode)
            onMessage?.invoke("🔧 **ИСПРАВЛЕННЫЙ КОД СОХРАНЕН:** $fixedFilePath")

            // 6. Генерируем тесты для исходного кода (собственная логика)
            onMessage?.invoke("📊 **СОЗДАЮ ТЕСТЫ С ПОМОЩЬЮ СОБСТВЕННОЙ ЛОГИКИ...**")
            val testCode = testGenerationService.generateTestsManually(sourceCode, fileName, onMessage)

            // 7. Записываем тесты в файл
            val testFilePath = filePath.replace(".kt", "_Test.kt")
            fileService.writeFile(testFilePath, testCode)
            onMessage?.invoke("💾 **ТЕСТЫ СОХРАНЕНЫ:** $testFilePath")

            // 8. Запускаем тесты
            onMessage?.invoke("▶️ **ЗАПУСКАЮ ТЕСТЫ...**")
            val projectDir = filePath.substringBeforeLast("/")
            val testResult = testExecutionService.executeTests(testFilePath, projectDir)
            onMessage?.invoke("✅ **ТЕСТЫ ВЫПОЛНЕНЫ**")
            
            // 9. Формируем итоговый отчет
            val finalReport = generateFinalReport(bugAnalysis, testCode, testResult, fileName)
            
            // Анализ багов уже показан выше отдельным сообщением
            
            return finalReport
            
        } catch (e: Exception) {
            // Специальная обработка для случаев, когда AI модель не справилась
            val errorMessage = when (e) {
                is com.tayrinn.aiadvent.data.api.AIModelFailureException -> {
                    "🤖 **МОДЕЛЬ AI НЕ СМОГЛА СПРАВИТЬСЯ С ЗАДАЧЕЙ**\n\n" +
                    "Причина: ${e.message}\n\n" +
                    "Рекомендации:\n" +
                    "• Попробуйте упростить код или разбить его на части\n" +
                    "• Попробуйте позже - возможно, модель перегружена\n" +
                    "• Проверьте, что код компилируется и не содержит синтаксических ошибок"
                }
                else -> "❌ Ошибка при обработке файла: ${e.message}"
            }

            onMessage?.invoke(errorMessage)
            return errorMessage
        }
    }

    /**
     * Генерирует сообщение с анализом багов для отдельного вывода
     */
    private fun generateBugAnalysisMessage(bugAnalysis: BugAnalysis, fileName: String): String {
        val message = StringBuilder()
        
        message.appendLine("🔍 **АНАЛИЗ БАГОВ ДЛЯ ФАЙЛА: $fileName**")
        message.appendLine("=".repeat(60))
        
        if (bugAnalysis.bugs.isNotEmpty()) {
            message.appendLine("🐛 **НАЙДЕННЫЕ ПРОБЛЕМЫ (${bugAnalysis.bugs.size}):**")
            bugAnalysis.bugs.forEach { bug ->
                val severityIcon = when (bug.severity) {
                    "high" -> "🔴"
                    "medium" -> "🟡"
                    "low" -> "🟢"
                    else -> "⚪"
                }
                message.appendLine("$severityIcon Строка ${bug.line}: ${bug.type}")
                message.appendLine("   ${bug.description}")
                message.appendLine()
            }
            
            message.appendLine("🔧 **ПРЕДЛОЖЕННЫЕ ИСПРАВЛЕНИЯ (${bugAnalysis.fixes.size}):**")
            bugAnalysis.fixes.forEach { fix ->
                message.appendLine("📝 Строка ${fix.line}:")
                message.appendLine("   ${fix.explanation}")
                message.appendLine("   ${fix.fix}")
                message.appendLine()
            }
        } else {
            message.appendLine("✅ **ПРОБЛЕМ НЕ НАЙДЕНО**")
            message.appendLine("Код соответствует стандартам качества")
        }
        
        message.appendLine("📋 **ИТОГ:** ${bugAnalysis.summary}")
        message.appendLine("=".repeat(60))
        
        return message.toString()
    }

    /**
     * Генерирует отчет о найденных багах
     */
    private fun generateBugReport(bugAnalysis: BugAnalysis): String {
        val report = StringBuilder()
        
        report.appendLine("🔍 **АНАЛИЗ КОДА НА БАГИ**")
        report.appendLine("=".repeat(50))
        
        if (bugAnalysis.bugs.isNotEmpty()) {
            report.appendLine("🐛 **НАЙДЕННЫЕ ПРОБЛЕМЫ:**")
            bugAnalysis.bugs.forEach { bug ->
                val severityIcon = when (bug.severity) {
                    "high" -> "🔴"
                    "medium" -> "🟡"
                    "low" -> "🟢"
                    else -> "⚪"
                }
                report.appendLine("$severityIcon **Строка ${bug.line}** - ${bug.type}")
                report.appendLine("   ${bug.description}")
                report.appendLine()
            }
            
            report.appendLine("🔧 **ПРЕДЛОЖЕННЫЕ ИСПРАВЛЕНИЯ:**")
            bugAnalysis.fixes.forEach { fix ->
                report.appendLine("📝 **Строка ${fix.line}**")
                report.appendLine("   ${fix.explanation}")
                report.appendLine("   ```kotlin")
                report.appendLine("   ${fix.fix}")
                report.appendLine("   ```")
                report.appendLine()
            }
        } else {
            report.appendLine("✅ **ПРОБЛЕМ НЕ НАЙДЕНО**")
            report.appendLine("Код соответствует стандартам качества")
        }
        
        report.appendLine("📋 **ИТОГ:** ${bugAnalysis.summary}")
        report.appendLine()
        
        return report.toString()
    }

    /**
     * Генерирует итоговый отчет
     */
    private fun generateFinalReport(
        bugAnalysis: BugAnalysis,
        testCode: String,
        testResult: String,
        fileName: String
    ): String {
        val report = StringBuilder()
        
        report.appendLine("🚀 **ИТОГОВЫЙ ОТЧЕТ ПО ФАЙЛУ: $fileName**")
        report.appendLine("=".repeat(60))
        report.appendLine()
        
        // Отчет о багах
        report.appendLine("🔍 **АНАЛИЗ БАГОВ:**")
        if (bugAnalysis.bugs.isNotEmpty()) {
            report.appendLine("🐛 **НАЙДЕННЫЕ ПРОБЛЕМЫ:**")
            bugAnalysis.bugs.forEach { bug ->
                val severityIcon = when (bug.severity) {
                    "high" -> "🔴"
                    "medium" -> "🟡"
                    "low" -> "🟢"
                    else -> "⚪"
                }
                report.appendLine("$severityIcon **Строка ${bug.line}** - ${bug.type}")
                report.appendLine("   ${bug.description}")
                report.appendLine()
            }
            
            report.appendLine("🔧 **ПРЕДЛОЖЕННЫЕ ИСПРАВЛЕНИЯ:**")
            bugAnalysis.fixes.forEach { fix ->
                report.appendLine("📝 **Строка ${fix.line}**")
                report.appendLine("   ${fix.explanation}")
                report.appendLine("   ```kotlin")
                report.appendLine("   ${fix.fix}")
                report.appendLine("   ```")
                report.appendLine()
            }
        } else {
            report.appendLine("✅ **ПРОБЛЕМ НЕ НАЙДЕНО**")
            report.appendLine("Код соответствует стандартам качества")
        }
        report.appendLine("📋 **ИТОГ:** ${bugAnalysis.summary}")
        report.appendLine()
        
        // Отчет о тестах
        report.appendLine("🧪 **СГЕНЕРИРОВАННЫЕ ТЕСТЫ:**")
        report.appendLine("Тесты записаны в файл: ${fileName.replace(".kt", "_Test.kt")}")
        report.appendLine()
        
        // Результат выполнения тестов
        report.appendLine("▶️ **РЕЗУЛЬТАТ ВЫПОЛНЕНИЯ ТЕСТОВ:**")
        report.appendLine(testResult)
        report.appendLine()
        
        // Рекомендации
        report.appendLine("💡 **РЕКОМЕНДАЦИИ:**")
        if (bugAnalysis.bugs.isNotEmpty()) {
            report.appendLine("• Исправьте найденные проблемы перед использованием кода в продакшене")
            report.appendLine("• Запустите тесты после исправления багов")
            report.appendLine("• Рассмотрите использование линтеров для предотвращения подобных проблем")
        } else {
            report.appendLine("• Код готов к использованию")
            report.appendLine("• Регулярно запускайте тесты для поддержания качества")
        }
        
        return report.toString()
    }
}
