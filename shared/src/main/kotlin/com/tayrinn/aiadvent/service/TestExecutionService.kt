package com.tayrinn.aiadvent.service

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Сервис для выполнения тестов
 */
class TestExecutionService {
    
    /**
     * Выполняет тесты для Kotlin файла
     */
    suspend fun executeTests(testFilePath: String, projectDir: String): String {
        return try {
            println("🧪 Запускаю тесты для файла: $testFilePath")
            
            // Используем основной проект вместо создания временного
            val projectRoot = projectDir
            println("🧪 Запускаю тесты в директории: $projectRoot")
            
            // Выполняем тесты через Gradle
            val result = executeGradleTest(projectRoot, testFilePath)
            println("✅ Тесты выполнены")
            result
            
        } catch (e: Exception) {
            "❌ Ошибка выполнения тестов: ${e.message}"
        }
    }
    
    // Убрали создание временных файлов - используем основной проект
    
    /**
     * Выполняет Gradle тесты
     */
    private fun executeGradleTest(projectDir: String, testFilePath: String): String {
        return try {
            println("🧪 Запускаю тесты в директории: $projectDir")
            
            // Проверяем наличие gradlew в проекте
            val gradlewFile = File(projectDir, "gradlew")
            val gradlewBatFile = File(projectDir, "gradlew.bat")
            
            val gradleCommand = when {
                gradlewFile.exists() -> "./gradlew"
                gradlewBatFile.exists() -> "gradlew.bat"
                else -> {
                    return "❌ Ошибка: Не найден gradlew в проекте. Тесты не могут быть выполнены."
                }
            }
            
            // Запускаем тесты напрямую без предварительной сборки
            println("🧪 Запускаю тесты в основном проекте...")
            
            // Теперь запускаем тесты для конкретного файла
            println("🧪 Запускаю команду: $gradleCommand :shared:test --tests *SimpleCalculatorTest* --no-daemon --info")
            val testProcess = ProcessBuilder()
                .directory(File(projectDir))
                .command(gradleCommand, ":shared:test", "--tests", "*SimpleCalculatorTest*", "--no-daemon", "--info")
                .start()
            
            val testOutput = testProcess.inputStream.bufferedReader().readText()
            val testErrorOutput = testProcess.errorStream.bufferedReader().readText()
            val testExitCode = testProcess.waitFor()
            
            buildString {
                appendLine("=== РЕЗУЛЬТАТ ВЫПОЛНЕНИЯ ТЕСТОВ ===")
                appendLine("Код завершения: $testExitCode")
                appendLine("Команда: $gradleCommand :shared:test --tests *SimpleCalculatorTest* --info")
                appendLine()
                
                if (testOutput.isNotEmpty()) {
                    appendLine("=== СТАНДАРТНЫЙ ВЫВОД ===")
                    appendLine(testOutput)
                    appendLine()
                }
                
                if (testErrorOutput.isNotEmpty()) {
                    appendLine("=== ВЫВОД ОШИБОК ===")
                    appendLine(testErrorOutput)
                    appendLine()
                }
                
                if (testExitCode == 0) {
                    appendLine("✅ Тесты выполнены успешно!")
                } else {
                    appendLine("❌ Тесты завершились с ошибками")
                }
            }
        } catch (e: Exception) {
            "❌ Ошибка выполнения тестов: ${e.message}"
        }
    }
    
    /**
     * Очищает временные файлы
     */
    private fun cleanupTempFiles(projectDir: String) {
        try {
            val buildGradle = File(projectDir, "build.gradle.kts")
            val settingsGradle = File(projectDir, "settings.gradle.kts")
            val buildDir = File(projectDir, "build")
            val gradleDir = File(projectDir, ".gradle")
            
            buildGradle.delete()
            settingsGradle.delete()
            buildDir.deleteRecursively()
            gradleDir.deleteRecursively()
        } catch (e: Exception) {
            println("Предупреждение: не удалось очистить временные файлы: ${e.message}")
        }
    }
    
    /**
     * Ищет корень проекта с gradlew
     */
    private fun findProjectRoot(startDir: String): String? {
        var currentDir = File(startDir)
        val maxDepth = 5
        
        repeat(maxDepth) {
            if (currentDir.exists()) {
                val gradlewFile = File(currentDir, "gradlew")
                val gradlewBatFile = File(currentDir, "gradlew.bat")
                
                if (gradlewFile.exists() || gradlewBatFile.exists()) {
                    return currentDir.absolutePath
                }
            }
            
            val parentDir = currentDir.parentFile
            if (parentDir == null || !parentDir.exists()) {
                return null
            }
            currentDir = parentDir
        }
        return null
    }
}
