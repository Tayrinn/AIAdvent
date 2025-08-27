package com.example

import org.junit.Test
import org.junit.Assert.*

class TestSimple {

    @Test
    fun testAddition() {
        val result = 2 + 3
        assertEquals(5, result)
    }

    @Test
    fun testString() {
        val str = "hello"
        assertNotNull(str)
        assertEquals("hello", str)
    }
}
