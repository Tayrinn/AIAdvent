package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.model.BugAnalysis
import com.tayrinn.aiadvent.data.model.Bug
import com.tayrinn.aiadvent.data.model.Fix
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class TestWorkflowServiceTest {
    
    @Test
    fun `test processFile reads file and generates tests`() = runTest {
        // Arrange
        val mockBugFixService = mock(BugFixService::class.java)
        val mockTestGenerationService = mock(TestGenerationService::class.java)
        val mockTestExecutionService = mock(TestExecutionService::class.java)
        val mockFileService = mock(FileService::class.java)
        
        val testSourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val testFileName = "TestFile.kt"
        val testFilePath = "/path/to/TestFile.kt"
        
        val mockBugAnalysis = BugAnalysis(
            bugs = listOf(Bug(1, "test", "test", "low")),
            fixes = listOf(Fix(1, "test", "test")),
            summary = "Test summary"
        )
        
        val generatedTests = """
            @Test
            fun testAdd() {
                assertEquals(3, add(1, 2))
            }
        """.trimIndent()
        
        val testResult = "Tests passed"
        
        // Mock responses
        whenever(mockFileService.readFile(testFilePath)).thenReturn(testSourceCode)
        whenever(mockFileService.getFileName(testFilePath)).thenReturn(testFileName)
        whenever(mockBugFixService.analyzeAndFixBugs(testSourceCode)).thenReturn(mockBugAnalysis)
        whenever(mockTestGenerationService.generateTestsManually(testSourceCode, testFileName)).thenReturn(generatedTests)
        whenever(mockTestExecutionService.executeTests(any(), any())).thenReturn(testResult)
        whenever(mockFileService.writeFile(any(), any())).thenReturn(true)
        
        val testWorkflowService = TestWorkflowService(
            mockFileService,
            mockBugFixService,
            mockTestGenerationService,
            mockTestExecutionService
        )
        
        // Act
        val result = testWorkflowService.executeTestWorkflow(testFilePath)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.contains("ИТОГОВЫЙ ОТЧЕТ ПО ФАЙЛУ: TestFile.kt"))
        assertTrue(result.contains("Test summary"))
        assertTrue(result.contains("Тесты записаны в файл: TestFile_Test.kt"))
        
        // Verify interactions
        verify(mockFileService).readFile(testFilePath)
        verify(mockFileService).getFileName(testFilePath)
        verify(mockBugFixService).analyzeAndFixBugs(testSourceCode)
        verify(mockTestGenerationService).generateTestsManually(testSourceCode, testFileName)
        verify(mockFileService).writeFile(eq("/path/to/TestFile_Test.kt"), eq(generatedTests))
        verify(mockTestExecutionService).executeTests(eq("/path/to/TestFile_Test.kt"), eq("/path/to"))
    }
}
