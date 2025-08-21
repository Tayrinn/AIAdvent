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
            val configFile = File(configPath)
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    properties.load(input)
                }
                isLoaded = true
                println("✅ Конфигурация загружена из: $configPath")
            } else {
                println("⚠️ Файл конфигурации не найден: $configPath")
                loadDefaultConfig()
            }
        } catch (e: Exception) {
            println("❌ Ошибка загрузки конфигурации: ${e.message}")
            loadDefaultConfig()
        }
    }
    
    /**
     * Загружает конфигурацию по умолчанию
     */
    private fun loadDefaultConfig() {
        properties.setProperty("openai.api.key", "")
        properties.setProperty("openai.api.model", "gpt-3.5-turbo")
        properties.setProperty("openai.api.max_tokens", "2000")
        properties.setProperty("openai.api.temperature", "0.7")
        println("📋 Загружена конфигурация по умолчанию")
    }
    
    /**
     * Получает значение конфигурации
     */
    fun getProperty(key: String, defaultValue: String = ""): String {
        if (!isLoaded) {
            loadConfig()
        }
        return properties.getProperty(key, defaultValue)
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
}
