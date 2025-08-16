package com.tayrinn.aiadvent.data.model

data class ApiLimits(
    val remainingGenerations: Int,
    val totalGenerations: Int,
    val resetDate: String? = null
) {
    val usedGenerations: Int
        get() = totalGenerations - remainingGenerations
    
    val usagePercentage: Float
        get() = (usedGenerations.toFloat() / totalGenerations.toFloat()) * 100
    
    val isLimitReached: Boolean
        get() = remainingGenerations <= 0
    
    val remainingPercentage: Float
        get() = (remainingGenerations.toFloat() / totalGenerations.toFloat()) * 100
}
