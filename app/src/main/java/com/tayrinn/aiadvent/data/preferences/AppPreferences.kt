package com.tayrinn.aiadvent.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class AppPreferences(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "aiadvent_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_OLLAMA_IP = "ollama_ip"
        private const val KEY_OLLAMA_PORT = "ollama_port"
        private const val KEY_HOST_IP = "host_ip"
        private const val DEFAULT_OLLAMA_IP = "192.168.1.6"
        private const val DEFAULT_OLLAMA_PORT = "11434"
        private const val DEFAULT_HOST_IP = "192.168.1.6"
    }

    fun getOllamaIp(): String {
        return prefs.getString(KEY_OLLAMA_IP, DEFAULT_OLLAMA_IP) ?: DEFAULT_OLLAMA_IP
    }

    fun setOllamaIp(ip: String) {
        prefs.edit().putString(KEY_OLLAMA_IP, ip).apply()
    }

    fun getOllamaPort(): String {
        return prefs.getString(KEY_OLLAMA_PORT, DEFAULT_OLLAMA_PORT) ?: DEFAULT_OLLAMA_PORT
    }

    fun setOllamaPort(port: String) {
        prefs.edit().putString(KEY_OLLAMA_PORT, port).apply()
    }

    fun getHostIp(): String {
        return prefs.getString(KEY_HOST_IP, DEFAULT_HOST_IP) ?: DEFAULT_HOST_IP
    }

    fun setHostIp(ip: String) {
        prefs.edit().putString(KEY_HOST_IP, ip).apply()
    }

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
}
