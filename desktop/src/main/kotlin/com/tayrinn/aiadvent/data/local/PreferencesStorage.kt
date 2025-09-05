package com.tayrinn.aiadvent.data.local

import com.tayrinn.aiadvent.data.model.UserPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class PreferencesStorage {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val preferencesDir = File(System.getProperty("user.home"), ".aiadvent/preferences")
    
    init {
        // Создаем директорию для предпочтений если её нет
        if (!preferencesDir.exists()) {
            preferencesDir.mkdirs()
            println("📁 Создана директория для предпочтений: ${preferencesDir.absolutePath}")
        }
    }
    
    /**
     * Сохраняет предпочтения пользователя в файл
     */
    suspend fun savePreferences(preferences: UserPreferences): Boolean = withContext(Dispatchers.IO) {
        try {
            val preferencesFile = File(preferencesDir, "${preferences.userId}_preferences.json")
            val jsonString = json.encodeToString(preferences)
            
            preferencesFile.writeText(jsonString)
            println("💾 Предпочтения сохранены: ${preferencesFile.absolutePath}")
            println("📋 Сохраненные данные: $jsonString")
            true
        } catch (e: IOException) {
            println("❌ Ошибка сохранения предпочтений: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Загружает предпочтения пользователя из файла
     */
    suspend fun loadPreferences(userId: String): UserPreferences? = withContext(Dispatchers.IO) {
        try {
            val preferencesFile = File(preferencesDir, "${userId}_preferences.json")
            
            if (!preferencesFile.exists()) {
                println("📂 Файл предпочтений не найден для пользователя: $userId")
                return@withContext null
            }
            
            val jsonString = preferencesFile.readText()
            val preferences = json.decodeFromString<UserPreferences>(jsonString)
            
            println("📖 Предпочтения загружены для пользователя: $userId")
            println("📋 Загруженные данные: $jsonString")
            preferences
        } catch (e: Exception) {
            println("❌ Ошибка загрузки предпочтений: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Обновляет существующие предпочтения или создает новые
     */
    suspend fun updatePreferences(
        userId: String,
        updater: (UserPreferences?) -> UserPreferences
    ): UserPreferences? = withContext(Dispatchers.IO) {
        try {
            val currentPreferences = loadPreferences(userId)
            val updatedPreferences = updater(currentPreferences)
            
            if (savePreferences(updatedPreferences)) {
                updatedPreferences
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка обновления предпочтений: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Удаляет предпочтения пользователя
     */
    suspend fun deletePreferences(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val preferencesFile = File(preferencesDir, "${userId}_preferences.json")
            
            if (preferencesFile.exists()) {
                val deleted = preferencesFile.delete()
                if (deleted) {
                    println("🗑️ Предпочтения удалены для пользователя: $userId")
                } else {
                    println("❌ Не удалось удалить предпочтения для пользователя: $userId")
                }
                deleted
            } else {
                println("📂 Файл предпочтений не найден для пользователя: $userId")
                true // Считаем это успехом, так как цель достигнута
            }
        } catch (e: Exception) {
            println("❌ Ошибка удаления предпочтений: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Получает список всех пользователей с сохраненными предпочтениями
     */
    suspend fun getAllUserIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            preferencesDir.listFiles { file ->
                file.isFile && file.name.endsWith("_preferences.json")
            }?.map { file ->
                file.name.removeSuffix("_preferences.json")
            } ?: emptyList()
        } catch (e: Exception) {
            println("❌ Ошибка получения списка пользователей: ${e.message}")
            emptyList()
        }
    }
}
