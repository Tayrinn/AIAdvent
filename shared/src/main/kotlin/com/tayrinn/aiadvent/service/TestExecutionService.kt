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
    suspend fun executeKotlinTests(testFilePath: String, projectDir: String): String {
        return try {
            // Создаем временный build.gradle.kts для тестов
            val buildGradleContent = createTestBuildGradle()
            val buildGradlePath = "$projectDir/build.gradle.kts"
            Files.write(Paths.get(buildGradlePath), buildGradleContent.toByteArray())
            
            // Создаем временный settings.gradle.kts
            val settingsGradleContent = createTestSettingsGradle()
            val settingsGradlePath = "$projectDir/settings.gradle.kts"
            Files.write(Paths.get(settingsGradlePath), settingsGradleContent.toByteArray())
            
            // Выполняем тесты через Gradle
            val result = executeGradleTest(projectDir)
            
            // Очищаем временные файлы
            cleanupTempFiles(projectDir)
            
            result
        } catch (e: Exception) {
            "Ошибка выполнения тестов: ${e.message}"
        }
    }
    
    /**
     * Создает временный build.gradle.kts для тестов
     */
    private fun createTestBuildGradle(): String {
        return """
plugins {
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.10")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(17)
}
        """.trimIndent()
    }
    
    /**
     * Создает временный settings.gradle.kts
     */
    private fun createTestSettingsGradle(): String {
        return """
rootProject.name = "temp-test-project"
        """.trimIndent()
    }
    
    /**
     * Выполняет Gradle тесты
     */
    private fun executeGradleTest(projectDir: String): String {
        return try {
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
            
            val processBuilder = ProcessBuilder()
                .directory(File(projectDir))
                .command(gradleCommand, "test", "--info")
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            
            val exitCode = process.waitFor()
            
            buildString {
                appendLine("=== РЕЗУЛЬТАТ ВЫПОЛНЕНИЯ ТЕСТОВ ===")
                appendLine("Код завершения: $exitCode")
                appendLine("Команда: $gradleCommand test --info")
                appendLine()
                
                if (output.isNotEmpty()) {
                    appendLine("=== СТАНДАРТНЫЙ ВЫВОД ===")
                    appendLine(output)
                    appendLine()
                }
                
                if (errorOutput.isNotEmpty()) {
                    appendLine("=== ВЫВОД ОШИБОК ===")
                    appendLine(errorOutput)
                    appendLine()
                }
                
                if (exitCode == 0) {
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
     * Выполняет тесты для других языков программирования
     */
    suspend fun executeTests(testFilePath: String, projectDir: String): String {
        val extension = testFilePath.substringAfterLast(".", "").lowercase()
                return when (extension) {
            "kt" -> executeKotlinTests(testFilePath, projectDir)
            "java" -> executeJavaTests(testFilePath, projectDir)
            "py" -> executePythonTests(testFilePath, projectDir)
            else -> "Неподдерживаемый язык программирования: $extension"
        }
    }
    
    /**
     * Выполняет Java тесты
     */
    private suspend fun executeJavaTests(testFilePath: String, projectDir: String): String {
        return "Выполнение Java тестов пока не реализовано"
    }
    
    /**
     * Выполняет Python тесты
     */
    private suspend fun executePythonTests(testFilePath: String, projectDir: String): String {
        return "Выполнение Python тестов пока не реализовано"
    }
}

