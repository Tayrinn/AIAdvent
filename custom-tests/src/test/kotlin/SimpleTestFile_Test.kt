package com.example

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

class SimpleTestFileTest {

@Test
fun testDivide() {
    val calculator = SimpleCalculator()

    // Тест с валидными значениями
    val result = calculator.divide(10.0, 2.0)
    assertEquals(5.0, result, 0.001)

    // Тест с null должен бросить исключение
    try {
        calculator.divide(10.0, null)
        fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
        // Ожидаемое исключение
    }
}

@Test
fun testDivideWithZero() {
    val calculator = SimpleCalculator()

    val result = calculator.divide(10.0, 0.0)
    assertEquals(Double.POSITIVE_INFINITY, result, 0.001)
}

@Test
fun testDivideWithNull() {
    val calculator = SimpleCalculator()

    try {
        calculator.divide(10.0, null)
        fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
        // Ожидаемое исключение
    }
}

@Test
fun testAdd() {
    val calculator = SimpleCalculator()

    // Базовый тест
    val result = calculator.add(5, 3)
    assertEquals(8, result)

    // Тест с нулями
    val result2 = calculator.add(0, 0)
    assertEquals(0, result2)
}

@Test
fun testAddWithZero() {
    val calculator = SimpleCalculator()

    val result = calculator.add(0, 0)
    assertEquals(0, result)
}

@Test
fun testAddWithNegative() {
    val calculator = SimpleCalculator()

    // Тест с отрицательными числами
    val result = calculator.add(-2, 3)
    assertEquals(1, result)

    // Тест с двумя отрицательными числами
    val result2 = calculator.add(-5, -3)
    assertEquals(-8, result2)
}

@Test
fun testProcessString() {
    val calculator = SimpleCalculator()

    // Тест с null
    val result1 = calculator.processString(null)
    assertEquals("empty", result1)
}

@Test
fun testProcessStringWithValue() {
    val calculator = SimpleCalculator()

    // Тест с обычной строкой
    val result2 = calculator.processString("hello")
    assertNotNull(result2)
}

@Test
fun testProcessStringWithSpaces() {
    val calculator = SimpleCalculator()

    // Тест со строкой с пробелами
    val result = calculator.processString("  hello  ")
    assertEquals("hello", result)

    // Тест с пустой строкой после trim
    val result2 = calculator.processString("   ")
    assertEquals("", result2)
}

@Test
fun testCountToFive() {
    val calculator = SimpleCalculator()

    val result = calculator.countToFive()

    // Проверяем, что результат не null
    assertNotNull(result)

    // Проверяем, что список содержит 5 элементов
    assertEquals(5, result.size)

    // Проверяем содержимое списка
    assertEquals(listOf(1, 2, 3, 4, 5), result)
}
}