package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.model.Bug
import com.tayrinn.aiadvent.data.model.BugAnalysis
import com.tayrinn.aiadvent.data.model.Fix
import com.tayrinn.aiadvent.data.repository.OpenAIChatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

class BugFixServiceTest {

    @Test
    fun `test analyzeAndFixBugs with max_tokens support`() = runTest {
        // Arrange
        val mockRepository = mock<OpenAIChatRepository>()
        val bugFixService = BugFixService(mockRepository)
        
        val testSourceCode = """
            fun testFunction() {
                val unusedVariable = "test"
                val result = 10 / 0  // Division by zero
                println(result)
            }
        """.trimIndent()
        
        // Mock repository response
        whenever(mockRepository.sendMessage(any(), any(), anyOrNull())).thenReturn(
            Pair("""
                {
                    "bugs": [
                        {
                            "line": 2,
                            "type": "unused_variable",
                            "description": "Variable 'unusedVariable' is declared but never used",
                            "severity": "low"
                        },
                        {
                            "line": 3,
                            "type": "exception_risk",
                            "description": "Division by zero will cause ArithmeticException",
                            "severity": "high"
                        }
                    ]
                }
            """.trimIndent(), "")
        )
        
        // Act
        val result = bugFixService.analyzeAndFixBugs(testSourceCode)
        
        // Assert
        assertNotNull(result)
        assertEquals(2, result.bugs.size)
        assertEquals(2, result.fixes.size)
        
        // Verify that sendMessage was called
        verify(mockRepository).sendMessage(
            any(),
            any(),  // recentMessages parameter
            anyOrNull()  // modelName parameter
        )
    }

    @Test
    fun `test createFixesFromBugs with different bug types`() = runTest {
        // Arrange
        val mockRepository = mock<OpenAIChatRepository>()
        val bugFixService = BugFixService(mockRepository)
        
        val bugs = listOf(
            Bug(1, "unsafe_operator", "Using !! operator", "high"),
            Bug(2, "logical_error", "Wrong logic", "medium"),
            Bug(3, "string_handling", "Missing trim()", "low")
        )
        
        // Act
        val fixes = bugFixService.createFixesFromBugs(bugs)
        
        // Assert
        assertEquals(3, fixes.size)
        
        val unsafeOperatorFix = fixes[0]
        assertEquals(1, unsafeOperatorFix.line)
        assertNotNull(unsafeOperatorFix.fix)
        
        val logicalErrorFix = fixes[1]
        assertEquals(2, logicalErrorFix.line)
        assertNotNull(logicalErrorFix.fix)
        
        val stringHandlingFix = fixes[2]
        assertEquals(3, stringHandlingFix.line)
        assertNotNull(stringHandlingFix.fix)
    }
}
