```kotlin
package com.example

class SimpleCalculator {

    fun divide(a: Double, b: Double?): Double {
        val denom = b ?: throw IllegalArgumentException("b must not be null")
        return a / denom
    }

    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun processString(str: String?): String {
        if (str == null) {
            return "empty"
        }
        return str.trim()
    }

    fun countToFive(): List<Int> {
        val result = mutableListOf<Int>()
        var i = 1
        while (i <= 5) {
            result.add(i)
            i++
        }
        return result
    }
}
```