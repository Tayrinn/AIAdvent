package com.example

import org.junit.Test
import org.junit.Assert.*

class SimpleTestFileTest {

@Test
fun testDivide() {
    val calculator = SimpleCalculator()

    // Тест с валидными значениями
    val result = calculator.divide(10.0, 2.0)
    assertEquals(5.0, result, 0.0)
}

@Test
fun testDivideWithZero() {
    val calculator = SimpleCalculator()

    val result = calculator.divide(10.0, 0.0)
    assertEquals(Double.POSITIVE_INFINITY, result, 0.0)
}

@Test(expected = NullPointerException::class)
fun testDivideWithNull() {
    val calculator = SimpleCalculator()

    calculator.divide(10.0, null)
}

@Test
fun testAdd() {
    val calculator = SimpleCalculator()

    // Базовый тест
    val result = calculator.add(5, 3)
    assertEquals(8, result)
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

    val result = calculator.add(-2, 3)
    assertEquals(1, result)
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
    val result3 = calculator.processString("  hello  ")
    assertEquals("hello", result3)
}

@Test
fun testCountToFive() {
    val calculator = SimpleCalculator()

    // Базовый тест для функции countToFive
    // TODO: Дополнить тест в соответствии с логикой функции
    // Сигнатура: fun countToFive()

    assertNotNull(calculator)
}
}