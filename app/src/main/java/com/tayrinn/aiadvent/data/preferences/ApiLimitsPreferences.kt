package com.tayrinn.aiadvent.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLimitsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "api_limits_prefs"
        private const val KEY_REMAINING_GENERATIONS = "remaining_generations"
        private const val KEY_TOTAL_GENERATIONS = "total_generations"
        private const val KEY_RESET_DATE = "reset_date"
        private const val DEFAULT_TOTAL_GENERATIONS = 100
        private const val DEFAULT_REMAINING_GENERATIONS = 91
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRemainingGenerations(): Int {
        return prefs.getInt(KEY_REMAINING_GENERATIONS, DEFAULT_REMAINING_GENERATIONS)
    }

    fun setRemainingGenerations(count: Int) {
        prefs.edit().putInt(KEY_REMAINING_GENERATIONS, count).apply()
    }

    fun getTotalGenerations(): Int {
        return prefs.getInt(KEY_TOTAL_GENERATIONS, DEFAULT_TOTAL_GENERATIONS)
    }

    fun setTotalGenerations(count: Int) {
        prefs.edit().putInt(KEY_TOTAL_GENERATIONS, count).apply()
    }

    fun getResetDate(): String? {
        return prefs.getString(KEY_RESET_DATE, null)
    }

    fun setResetDate(date: String?) {
        prefs.edit().putString(KEY_RESET_DATE, date).apply()
    }

    fun decreaseRemainingGenerations() {
        val current = getRemainingGenerations()
        if (current > 0) {
            setRemainingGenerations(current - 1)
        }
    }

    fun resetToDefault() {
        setRemainingGenerations(DEFAULT_REMAINING_GENERATIONS)
        setTotalGenerations(DEFAULT_TOTAL_GENERATIONS)
        setResetDate(null)
    }
}
