package com.example

import java.util.*

// Файл с преднамеренными багами для тестирования
class BuggyCalculator {
    
    // Баг 1: Неиспользуемая переменная
    private val unusedVariable = "never used"
    
    // Баг 2: Потенциальный null pointer exception
    fun divide(a: Double, b: Double?): Double {
        return a / b!!  // Опасное использование !!
    }
    
    // Баг 3: Логическая ошибка в сложении
    fun add(a: Int, b: Int): Int {
        return a - b  // Должно быть a + b
    }
    
    // Баг 4: Неправильная проверка на null
    fun processString(str: String?): String {
        if (str == null) {
            return "empty"
        }
        return str  // Должно быть str.trim() или другая обработка
    }
    
    // Баг 5: Бесконечный цикл
    fun countToTen(): List<Int> {
        val result = mutableListOf<Int>()
        var i = 1
        while (i <= 10) {
            result.add(i)
            // i++ отсутствует - бесконечный цикл!
        }
        return result
    }
    
    // Баг 6: Неправильное использование try-catch
    fun safeDivide(a: Double, b: Double): Double {
        try {
            return a / b
        } catch (e: Exception) {
            // Пустой catch блок - плохая практика
        }
        return 0.0
    }
    
    // Баг 7: Неэффективная работа с коллекциями
    fun findMax(numbers: List<Int>): Int? {
        if (numbers.isEmpty()) return null
        
        var max = numbers[0]
        for (i in 1 until numbers.size) {
            if (numbers[i] > max) {
                max = numbers[i]
            }
        }
        return max
        // Можно использовать numbers.maxOrNull()
    }
    
    // Баг 8: Отсутствие документации
    fun complexCalculation(x: Double, y: Double, z: Double): Double {
        // Сложная формула без комментариев
        return x * y + z / (x - y) * Math.sqrt(z) + Math.pow(x, 2) - Math.log(y)
    }
    
    // Баг 9: Небезопасное приведение типов
    fun processObject(obj: Any): String {
        val list = obj as List<String>  // Опасное приведение
        return list.joinToString()
    }
    
    // Баг 10: Утечка ресурсов
    fun readFile(filename: String): String {
        val file = java.io.File(filename)
        val reader = file.inputStream().bufferedReader()
        return reader.readText()
        // reader.close() отсутствует
    }
}


