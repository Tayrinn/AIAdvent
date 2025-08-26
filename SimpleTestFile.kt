package com.example

// Простой файл с преднамеренными багами для тестирования
class SimpleCalculator {

    // Баг 1: Неиспользуемая переменная
    private val unusedVar = "never used"

    // Баг 2: Потенциальный null pointer exception
    fun divide(a: Double, b: Double?): Double {
        return a / b!!  // Опасное использование !!
    }

    // Баг 3: Логическая ошибка
    fun add(a: Int, b: Int): Int {
        return a - b  // Должно быть a + b
    }

    // Баг 4: Неправильная проверка на null
    fun processString(str: String?): String {
        if (str == null) {
            return "empty"
        }
        return str  // Должно быть str.trim()
    }

    // Баг 5: Бесконечный цикл
    fun countToFive(): List<Int> {
        val result = mutableListOf<Int>()
        var i = 1
        while (i <= 5) {
            result.add(i)
            // i++ отсутствует - бесконечный цикл!
        }
        return result
    }
}
