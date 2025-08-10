package com.tayrinn.aiadvent.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREF_NAME = "ai_advent_prefs"
        private const val KEY_OLLAMA_IP = "ollama_ip"
        private const val KEY_OLLAMA_PORT = "ollama_port"
        private const val DEFAULT_IP = "192.168.1.6" // IP вашего компьютера
        private const val DEFAULT_PORT = "11434"
    }
    
    /**
     * Получает IP адрес Ollama сервера
     */
    fun getOllamaIp(): String {
        return prefs.getString(KEY_OLLAMA_IP, DEFAULT_IP) ?: DEFAULT_IP
    }
    
    /**
     * Устанавливает IP адрес Ollama сервера
     */
    fun setOllamaIp(ip: String) {
        prefs.edit().putString(KEY_OLLAMA_IP, ip).apply()
    }
    
    /**
     * Получает порт Ollama сервера
     */
    fun getOllamaPort(): String {
        return prefs.getString(KEY_OLLAMA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }
    
    /**
     * Устанавливает порт Ollama сервера
     */
    fun setOllamaPort(port: String) {
        prefs.edit().putString(KEY_OLLAMA_PORT, port).apply()
    }
    
    /**
     * Получает полный URL для Ollama API
     */
    fun getOllamaBaseUrl(): String {
        return "http://${getOllamaIp()}:${getOllamaPort()}/"
    }
    
    /**
     * Сбрасывает настройки на значения по умолчанию
     */
    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_OLLAMA_IP, DEFAULT_IP)
            .putString(KEY_OLLAMA_PORT, DEFAULT_PORT)
            .apply()
    }
}
