```kotlin
import com.tayrinn.aiadvent.examples.TrigonometryCalculator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrigonometryCalculatorTest {

    private val calculator = TrigonometryCalculator()

    @Test
    fun testSinDegrees() {
        assertEquals(0.5, calculator.sinDegrees(30.0))
        assertEquals(1.0, calculator.sinDegrees(90.0))
    }

    @Test
    fun testCosDegrees() {
        assertEquals(0.8660254037844386, calculator.cosDegrees(30.0))
        assertEquals(0.0, calculator.cosDegrees(90.0))
    }

    @Test
    fun testTanDegrees() {
        assertEquals(0.5773502691896257, calculator.tanDegrees(30.0))
        assertEquals(Double.POSITIVE_INFINITY, calculator.tanDegrees(90.0))
    }

    @Test
    fun testAsinDegrees() {
        assertEquals(30.0, calculator.asinDegrees(0.5))
        assertEquals(90.0, calculator.asinDegrees(1.0))
    }

    @Test
    fun testAcosDegrees() {
        assertEquals(60.0, calculator.acosDegrees(0.5))
        assertEquals(0.0, calculator.acosDegrees(1.0))
    }

    @Test
    fun testAtanDegrees() {
        assertEquals(45.0, calculator.atanDegrees(1.0))
        assertEquals(90.0, calculator.atanDegrees(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testHypotenuse() {
        assertEquals(5.0, calculator.hypotenuse(3.0, 4.0))
    }

    @Test
    fun testTriangleArea() {
        assertEquals(6.0, calculator.triangleArea(3.0, 4.0, 90.0))
    }

    @Test
    fun testIsRightTriangle() {
        assertTrue(calculator.isRightTriangle(3.0, 4.0, 5.0))
        assertFalse(calculator.isRightTriangle(3.0, 4.0, 6.0))
    }

    @Test
    fun testTrianglePerimeter() {
        assertEquals(12.0, calculator.trianglePerimeter(3.0, 4.0, 5.0))
    }

    @Test
    fun testAngleBetweenVectors() {
        assertEquals(90.0, calculator.angleBetweenVectors(1.0, 0.0, 0.0, 1.0))
        assertEquals(0.0, calculator.angleBetweenVectors(0.0, 0.0, 1.0, 1.0))
    }
}
```