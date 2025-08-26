package com.tayrinn.aiadvent.data.model

/**
 * Описание найденного бага
 */
data class Bug(
    val line: Int,
    val type: String,
    val description: String,
    val severity: String
)

/**
 * Описание исправления
 */
data class Fix(
    val line: Int,
    val fix: String,
    val explanation: String
)

/**
 * Результат анализа кода
 */
data class BugAnalysis(
    val bugs: List<Bug>,
    val fixes: List<Fix>,
    val summary: String
)


