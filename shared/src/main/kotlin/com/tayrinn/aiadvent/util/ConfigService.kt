package com.tayrinn.aiadvent.util

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Сервис для работы с конфигурационными файлами
 */
class ConfigService {
    
    private val properties = Properties()
    private var isLoaded = false
    
    /**
     * Загружает конфигурацию из файла
     */
    fun loadConfig(configPath: String = "config.properties") {
        try {
            println("🔍 Поиск файла конфигурации: $configPath")
            println("📁 Текущая рабочая директория: ${File(".").absolutePath}")
            
            // Сначала попробуем найти файл в текущей директории
            var configFile = File(configPath)
            println("🔍 Проверяем файл в текущей директории: ${configFile.absolutePath}")
            println("   Файл существует: ${configFile.exists()}")
            
            // Если не найден, попробуем найти в корне проекта
            if (!configFile.exists()) {
                println("🔍 Файл не найден в текущей директории, ищем в корне проекта...")
                val projectRoot = findProjectRoot()
                if (projectRoot != null) {
                    configFile = File(projectRoot, configPath)
                    println("🔍 Найден корень проекта: $projectRoot")
                    println("🔍 Проверяем файл: ${configFile.absolutePath}")
                    println("   Файл существует: ${configFile.exists()}")
                } else {
                    println("❌ Корень проекта не найден")
                }
            }
            
            // Попробуем абсолютный путь к корню проекта
            if (!configFile.exists()) {
                val absolutePath = "/Users/tayrinn/AndroidStudioProjects/AIAdvent/config.properties"
                configFile = File(absolutePath)
                println("🔍 Проверяем абсолютный путь: ${configFile.absolutePath}")
                println("   Файл существует: ${configFile.exists()}")
            }
            
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    properties.load(input)
                }
                isLoaded = true
                println("✅ Конфигурация загружена из: ${configFile.absolutePath}")
                println("📋 Загруженные ключи: ${properties.stringPropertyNames()}")
            } else {
                println("⚠️ Файл конфигурации не найден: $configPath")
                loadDefaultConfig()
            }
        } catch (e: Exception) {
            println("❌ Ошибка загрузки конфигурации: ${e.message}")
            e.printStackTrace()
            loadDefaultConfig()
        }
    }
    
    /**
     * Ищет корень проекта
     */
    private fun findProjectRoot(): String? {
        var currentDir = File(".").absolutePath
        val maxDepth = 10 // Максимальная глубина поиска
        
        println("🔍 Поиск корня проекта, начиная с: $currentDir")
        
        repeat(maxDepth) {
            val configFile = File(currentDir, "config.properties")
            println("   Проверяем: ${configFile.absolutePath} (существует: ${configFile.exists()})")
            
            if (configFile.exists()) {
                println("✅ Найден файл конфигурации в: $currentDir")
                return currentDir
            }
            
            val parentDir = File(currentDir).parentFile
            if (parentDir == null || !parentDir.exists()) {
                println("❌ Достигнут корень файловой системы")
                return null
            }
            currentDir = parentDir.absolutePath
        }
        
        println("❌ Достигнута максимальная глубина поиска")
        return null
    }
    
    /**
     * Загружает конфигурацию по умолчанию
     */
    private fun loadDefaultConfig() {
        // Hugging Face API configuration
        properties.setProperty("huggingface.api.key", "")
        properties.setProperty("huggingface.api.model", "deepseek-ai/DeepSeek-V3-0324")
        properties.setProperty("huggingface.api.max_tokens", "2000")
        properties.setProperty("huggingface.api.temperature", "0.7")

        // Legacy OpenAI configuration
        properties.setProperty("openai.api.key", "")
        properties.setProperty("openai.api.model", "gpt-5")
        properties.setProperty("openai.api.max_tokens", "2000")
        properties.setProperty("openai.api.temperature", "0.7")

        println("📋 Загружена конфигурация по умолчанию")
        println("⚠️ ВНИМАНИЕ: API ключи не загружены, будут использованы пустые значения!")
    }
    
    /**
     * Получает значение конфигурации
     */
    fun getProperty(key: String, defaultValue: String = ""): String {
        if (!isLoaded) {
            loadConfig()
        }
        val value = properties.getProperty(key, defaultValue)
        println("🔑 Получен ключ '$key': ${if (key.contains("key")) "***" else value}")
        return value
    }
    
    /**
     * Получает значение конфигурации как Int
     */
    fun getIntProperty(key: String, defaultValue: Int): Int {
        return getProperty(key, defaultValue.toString()).toIntOrNull() ?: defaultValue
    }
    
    /**
     * Получает значение конфигурации как Double
     */
    fun getDoubleProperty(key: String, defaultValue: Double): Double {
        return getProperty(key, defaultValue.toString()).toDoubleOrNull() ?: defaultValue
    }
    
    /**
     * Проверяет, загружена ли конфигурация
     */
    fun isConfigLoaded(): Boolean = isLoaded
    
    /**
     * Получает все доступные ключи конфигурации
     */
    fun getAvailableKeys(): Set<String> = properties.stringPropertyNames()

    /**
     * Получает Hugging Face API ключ
     */
    fun getHuggingFaceApiKey(): String = getProperty("huggingface.api.key", "")
}
