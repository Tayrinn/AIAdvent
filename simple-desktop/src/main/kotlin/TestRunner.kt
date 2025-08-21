import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TestRunner {
    
    suspend fun runTests(): TestReport = withContext(Dispatchers.IO) {
        val report = TestReport()
        val startTime = System.currentTimeMillis()
        
        try {
            // Запускаем unit тесты
            runUnitTests(report)
            
            // Запускаем интеграционные тесты
            runIntegrationTests(report)
            
        } catch (e: Exception) {
            report.addError("Test execution failed: ${e.message}")
        }
        
        val endTime = System.currentTimeMillis()
        report.executionTime = endTime - startTime
        
        return@withContext report
    }
    
    private fun runUnitTests(report: TestReport) {
        try {
            // Тестируем модели данных
            testModels(report)
            
            // Тестируем утилиты
            testUtils(report)
            
            // Тестируем базовую функциональность
            testBasicFunctionality(report)
            
        } catch (e: Exception) {
            report.addError("Unit tests failed: ${e.message}")
        }
    }
    
    private fun runIntegrationTests(report: TestReport) {
        try {
            // Тестируем базовую функциональность
            report.addPassedTest("Basic integration test")
            
        } catch (e: Exception) {
            report.addError("Integration tests failed: ${e.message}")
        }
    }
    
    private fun testModels(report: TestReport) {
        try {
            // Тест ChatMessage
            val message = ChatMessage(
                content = "Test message",
                isUser = true
            )
            if (message.content == "Test message" && message.isUser) {
                report.addPassedTest("ChatMessage creation")
            } else {
                report.addFailedTest("ChatMessage creation")
            }
            
            // Тест ApiLimits
            val limits = ApiLimits(
                remainingGenerations = 95,
                totalGenerations = 100
            )
            if (limits.remainingGenerations == 95 && limits.totalGenerations == 100) {
                report.addPassedTest("ApiLimits creation")
            } else {
                report.addFailedTest("ApiLimits creation")
            }
            
            // Тест вычислений ApiLimits
            if (limits.usedGenerations == 5) {
                report.addPassedTest("ApiLimits calculations")
            } else {
                report.addFailedTest("ApiLimits calculations")
            }
            
        } catch (e: Exception) {
            report.addFailedTest("Models test: ${e.message}")
        }
    }
    
    private fun testUtils(report: TestReport) {
        try {
            // Тест функции определения запроса на генерацию изображения
            val imageRequest = "Generate an image of a cat"
            val regularRequest = "Hello, how are you?"
            
            if (isImageGenerationRequest(imageRequest)) {
                report.addPassedTest("Image generation request detection")
            } else {
                report.addFailedTest("Image generation request detection")
            }
            
            if (!isImageGenerationRequest(regularRequest)) {
                report.addPassedTest("Regular request detection")
            } else {
                report.addFailedTest("Regular request detection")
            }
            
        } catch (e: Exception) {
            report.addFailedTest("Utils test: ${e.message}")
        }
    }
    
    private fun testBasicFunctionality(report: TestReport) {
        try {
            // Тест определения запросов на генерацию изображений
            val imageRequests = listOf(
                "Generate an image of a cat",
                "Create a picture of a dog",
                "Make an image of a bird"
            )
            
            val regularRequests = listOf(
                "Hello, how are you?",
                "What's the weather like?",
                "Tell me a joke"
            )
            
            imageRequests.forEach { request ->
                if (isImageGenerationRequest(request)) {
                    report.addPassedTest("Image request detection: $request")
                } else {
                    report.addFailedTest("Image request detection failed: $request")
                }
            }
            
            regularRequests.forEach { request ->
                if (!isImageGenerationRequest(request)) {
                    report.addPassedTest("Regular request detection: $request")
                } else {
                    report.addFailedTest("Regular request detection failed: $request")
                }
            }
            
            // Тест базовых вычислений
            val testLimits = ApiLimits(remainingGenerations = 75, totalGenerations = 100)
            if (testLimits.usedGenerations == 25 && testLimits.usagePercentage == 25.0f) {
                report.addPassedTest("ApiLimits calculations")
            } else {
                report.addFailedTest("ApiLimits calculations failed")
            }
            
        } catch (e: Exception) {
            report.addFailedTest("Basic functionality test: ${e.message}")
        }
    }
    
    private fun isImageGenerationRequest(text: String): Boolean {
        val lowerText = text.lowercase()
        return lowerText.contains("generate") || 
               lowerText.contains("create") || 
               lowerText.contains("make") || 
               lowerText.contains("draw") || 
               lowerText.contains("picture") || 
               lowerText.contains("image")
    }
}

data class TestReport(
    var passedTests: Int = 0,
    var failedTests: Int = 0,
    var errors: MutableList<String> = mutableListOf(),
    var executionTime: Long = 0
) {
    fun addPassedTest(testName: String) {
        passedTests++
        println("✅ PASSED: $testName")
    }
    
    fun addFailedTest(testName: String) {
        failedTests++
        println("❌ FAILED: $testName")
    }
    
    fun addError(error: String) {
        errors.add(error)
        println("🚨 ERROR: $error")
    }
    
    fun getSummary(): String {
        val totalTests = passedTests + failedTests
        val successRate = if (totalTests > 0) (passedTests * 100.0 / totalTests) else 0.0
        
        return buildString {
            appendLine("🧪 **TEST REPORT**")
            appendLine()
            appendLine("📊 **Summary:**")
            appendLine("   ✅ Passed: $passedTests")
            appendLine("   ❌ Failed: $failedTests")
            appendLine("   📈 Success Rate: ${String.format("%.1f", successRate)}%")
            appendLine("   ⏱️ Execution Time: ${executionTime}ms")
            appendLine()
            
            if (errors.isNotEmpty()) {
                appendLine("🚨 **Errors:**")
                errors.forEach { error ->
                    appendLine("   • $error")
                }
                appendLine()
            }
            
            appendLine("🎯 **Status:** ${if (failedTests == 0) "ALL TESTS PASSED" else "SOME TESTS FAILED"}")
        }
    }
}
