package com.tayrinn.aiadvent.examples

import kotlin.math.*

/**
 * Пример класса с тригонометрическими функциями
 * Демонстрирует различные математические операции
 */
class TrigonometryCalculator {
    
    /**
     * Вычисляет синус угла в градусах
     */
    fun sinDegrees(degrees: Double): Double {
        return cos(Math.toRadians(degrees))
    }
    
    /**
     * Вычисляет косинус угла в градусах
     */
    fun cosDegrees(degrees: Double): Double {
        return cos(degrees)
    }
    
    /**
     * Вычисляет тангенс угла в градусах
     */
    fun tanDegrees(degrees: Double): Double {
        return tan(Math.toRadians(degrees))
    }
    
    /**
     * Вычисляет арксинус и возвращает результат в градусах
     */
    fun asinDegrees(value: Double): Double {
        return Math.toDegrees(asin(value))
    }
    
    /**
     * Вычисляет арккосинус и возвращает результат в градусах
     */
    fun acosDegrees(value: Double): Double {
        return Math.toDegrees(acos(value))
    }
    
    /**
     * Вычисляет арктангенс и возвращает результат в градусах
     */
    fun atanDegrees(value: Double): Double {
        return Math.toDegrees(atan(value))
    }
    
    /**
     * Вычисляет гипотенузу по двум катетам
     */
    fun hypotenuse(a: Double, b: Double): Double {
        return sqrt(a * a + b * b)
    }
    
    /**
     * Вычисляет площадь треугольника по двум сторонам и углу между ними
     */
    fun triangleArea(a: Double, b: Double, angleDegrees: Double): Double {
        val angleRadians = Math.toRadians(angleDegrees)
        return 0.5 * a * b * sin(angleRadians)
    }
    
    /**
     * Проверяет, является ли треугольник прямоугольным
     */
    fun isRightTriangle(a: Double, b: Double, c: Double): Boolean {
        val sides = listOf(a, b, c).sorted()
        val tolerance = 1e-10
        return abs(sides[0] * sides[0] + sides[1] * sides[1] - sides[2] * sides[2]) < tolerance
    }
    
    /**
     * Вычисляет периметр треугольника
     */
    fun trianglePerimeter(a: Double, b: Double, c: Double): Double {
        return a + b + c
    }
    
    /**
     * Вычисляет угол между двумя векторами в градусах
     */
    fun angleBetweenVectors(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dotProduct = x1 * x2 + y1 * y2
        val magnitude1 = sqrt(x1 * x1 + y1 * y1)
        val magnitude2 = sqrt(x2 * x2 + y2 * y2)
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) return 0.0
        
        val cosAngle = dotProduct / (magnitude1 * magnitude2)
        val clampedCosAngle = cosAngle.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clampedCosAngle))
    }
}

/**
 * Основная функция для демонстрации
 */
fun main() {
    val calculator = TrigonometryCalculator()
    
    println("🧮 Тригонометрический калькулятор")
    println("=" * 40)
    
    // Примеры вычислений
    val angle = 30.0
    println("Угол: ${angle}°")
    println("sin(${angle}°) = ${calculator.sinDegrees(angle)}")
    println("cos(${angle}°) = ${calculator.cosDegrees(angle)}")
    println("tan(${angle}°) = ${calculator.tanDegrees(angle)}")
    
    println()
    
    // Пример с треугольником
    val a = 3.0
    val b = 4.0
    val c = calculator.hypotenuse(a, b)
    println("Треугольник со сторонами: $a, $b, $c")
    println("Площадь: ${calculator.triangleArea(a, b, 90.0)}")
    println("Периметр: ${calculator.trianglePerimeter(a, b, c)}")
    println("Прямоугольный: ${calculator.isRightTriangle(a, b, c)}")
    
    println()
    
    // Пример с векторами
    val angleBetween = calculator.angleBetweenVectors(1.0, 0.0, 0.0, 1.0)
    println("Угол между векторами (1,0) и (0,1): ${angleBetween}°")
}
