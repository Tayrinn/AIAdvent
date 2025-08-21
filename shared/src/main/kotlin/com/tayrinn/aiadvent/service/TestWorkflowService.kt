package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.OpenAIApi
import java.io.File

/**
 * Основной сервис для координации работы с тестами
 */
class TestWorkflowService(
    private val fileService: FileService,
    private val testGenerationService: TestGenerationService,
    private val testExecutionService: TestExecutionService
) {
    
    /**
     * Полный цикл работы с тестами для файла
     */
    suspend fun processFileForTesting(filePath: String): String {
        return try {
            // 1. Читаем исходный файл
            val sourceCode = fileService.readFile(filePath)
            if (sourceCode.startsWith("Ошибка")) {
                return sourceCode
            }
            
            val fileName = File(filePath).name
            if (!fileService.isSourceCodeFile(fileName)) {
                return "Файл $fileName не является исходным кодом"
            }
            
            // 2. Генерируем тесты через ChatGPT
            val generatedTests = testGenerationService.generateTests(sourceCode, fileName)
            if (generatedTests.startsWith("Ошибка")) {
                return generatedTests
            }
            
            // 3. Сохраняем тесты в файл
            val testFileName = fileService.generateTestFileName(fileName)
            val projectDir = File(filePath).parent
            val testDir = fileService.ensureTestDirectoryExists(projectDir)
            val testFilePath = "$testDir/$testFileName"
            
            val testSaved = fileService.writeFile(testFilePath, generatedTests)
            if (!testSaved) {
                return "Ошибка сохранения тестов в файл"
            }
            
            // 4. Анализируем код через ChatGPT
            val analysis = testGenerationService.analyzeTestResults(
                "Тесты сгенерированы и сохранены в файл: $testFilePath", 
                sourceCode
            )
            
            // 5. Формируем итоговый отчет
            createFinalReport(fileName, sourceCode, generatedTests, "Тесты сгенерированы успешно", analysis, filePath)
            
        } catch (e: Exception) {
            "Ошибка в процессе работы с тестами: ${e.message}"
        }
    }
    
    /**
     * Создает итоговый отчет
     */
    private fun createFinalReport(
        fileName: String,
        sourceCode: String,
        generatedTests: String,
        testResults: String,
        analysis: String,
        filePath: String
    ): String {
        val report = buildString {
            appendLine("📊 ОТЧЕТ О РАБОТЕ С ТЕСТАМИ")
            appendLine("=".repeat(50))
            appendLine("Файл: $fileName")
            appendLine("Время: ${java.time.LocalDateTime.now()}")
            appendLine()
            
            appendLine("📝 СГЕНЕРИРОВАННЫЕ ТЕСТЫ")
            appendLine("-".repeat(30))
            appendLine(generatedTests)
            appendLine()
            
            appendLine("🧪 РЕЗУЛЬТАТЫ ВЫПОЛНЕНИЯ ТЕСТОВ")
            appendLine("-".repeat(30))
            appendLine(testResults)
            appendLine()
            
            appendLine("🔍 АНАЛИЗ РЕЗУЛЬТАТОВ")
            appendLine("-".repeat(30))
            appendLine(analysis)
            appendLine()
            
            appendLine("=".repeat(50))
            appendLine("Отчет завершен")
        }
        
        // Сохраняем отчет в файл
        val reportFileName = "${fileName.substringBeforeLast(".")}_TestReport.txt"
        val reportPath = File(filePath).parent + "/" + reportFileName
        fileService.writeFile(reportPath, report)
        
        return report
    }
    
    /**
     * Быстрая проверка файла без выполнения тестов
     */
    suspend fun quickFileAnalysis(filePath: String): String {
        return try {
            val sourceCode = fileService.readFile(filePath)
            if (sourceCode.startsWith("Ошибка")) {
                return sourceCode
            }
            
            val fileName = File(filePath).name
            if (!fileService.isSourceCodeFile(fileName)) {
                return "Файл $fileName не является исходным кодом"
            }
            
            // Генерируем краткий анализ кода
            val analysis = testGenerationService.analyzeTestResults(
                "Анализ без выполнения тестов",
                sourceCode
            )
            
            analysis
        } catch (e: Exception) {
            "Ошибка быстрого анализа: ${e.message}"
        }
    }
}
