package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.OpenAIApi
import com.tayrinn.aiadvent.data.model.OpenAIMessage
import com.tayrinn.aiadvent.data.model.OpenAIRequest

/**
 * Сервис для генерации тестов через ChatGPT
 */
class TestGenerationService(
    private val openAIApi: OpenAIApi
) {
    
    /**
     * Генерирует тесты для исходного кода
     */
    suspend fun generateTests(sourceCode: String, fileName: String, onMessage: ((String) -> Unit)? = null): String {
        val prompt = createTestGenerationPrompt(sourceCode, fileName)
        
        // Добавляем логирование для отладки
        println("🔍 TestGenerationService.generateTests:")
        println("   File name: $fileName")
        println("   Source code length: ${sourceCode.length}")
        println("   Source code preview: ${sourceCode.take(200)}...")
        println("   Prompt length: ${prompt.length}")
        println("   Prompt preview: ${prompt.take(300)}...")
        
        val request = OpenAIRequest(
            model = "gpt-5",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = """Ты - эксперт по тестированию программного кода. 
Твоя задача - создавать качественные unit тесты для предоставленного кода.

Правила:
1. Создавай тесты на том же языке, что и исходный код
2. Используй стандартные фреймворки тестирования (JUnit для Java/Kotlin, pytest для Python, etc.)
3. Покрывай все публичные методы и функции
4. Включай позитивные и негативные тесты
5. Добавляй комментарии к сложным тестам
6. Используй понятные имена для тестов
7. Группируй связанные тесты в классы/функции
8. Возвращай только код тестов без дополнительных пояснений"""
                ),
                OpenAIMessage(
                    role = "user",
                    content = prompt
                )
            ),
            maxCompletionTokens = 2000
        )
        
        return try {
            println("🔄 Отправляем запрос на генерацию тестов...")
            onMessage?.invoke("🧪 **ГЕНЕРИРУЮ ТЕСТЫ...**")
            val response = openAIApi.chatCompletion(request)
            val rawContent = response.choices.firstOrNull()?.message?.content ?: ""
            println("✅ Сырой ответ от AI получен, длина: ${rawContent.length}")

            if (rawContent.isBlank()) {
                throw com.tayrinn.aiadvent.data.api.AIModelFailureException("Модель AI не смогла сгенерировать тесты и вернула пустой ответ. Возможно, код слишком сложный или модель перегружена.")
            }

            val testCode = rawContent

            // ПОКАЗЫВАЕМ СГЕНЕРИРОВАННЫЕ ТЕСТЫ ОТДЕЛЬНЫМ СООБЩЕНИЕМ
            val testMessage = """
🧪 **СГЕНЕРИРОВАННЫЕ ТЕСТЫ ОТ AI:**
${"=".repeat(60)}
$testCode
${"=".repeat(60)}
            """.trimIndent()
            onMessage?.invoke(testMessage)

            testCode
        } catch (e: Exception) {
            println("❌ Ошибка генерации тестов: ${e.message}")

            val errorMessage = if (e is com.tayrinn.aiadvent.data.api.AIModelFailureException) {
                "🤖 **МОДЕЛЬ AI НЕ СМОГЛА СГЕНЕРИРОВАТЬ ТЕСТЫ**\n\n" +
                "Причина: ${e.message}\n\n" +
                "Рекомендации:\n" +
                "• Попробуйте упростить код или выбрать меньшую часть\n" +
                "• Попробуйте позже - возможно, модель перегружена\n" +
                "• Используйте встроенную генерацию тестов (без AI)"
            } else {
                "❌ **ОШИБКА ГЕНЕРАЦИИ ТЕСТОВ:** ${e.message}"
            }

            onMessage?.invoke(errorMessage)
            "Ошибка генерации тестов: ${e.message}"
        }
    }
    
    /**
     * Создает промпт для генерации тестов с суммаризацией
     */
    private fun createTestGenerationPrompt(sourceCode: String, fileName: String): String {
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val language = when (extension) {
            "kt" -> "Kotlin"
            "java" -> "Java"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "cpp", "c" -> "C++"
            "cs" -> "C#"
            "go" -> "Go"
            "rs" -> "Rust"
            else -> "неизвестный язык"
        }
        
        // Суммаризуем код для уменьшения размера запроса
        val summarizedCode = summarizeCodeForTesting(sourceCode, language)

        val prompt = """Создай unit тесты для кода на $language.

Файл: $fileName
Код:
$summarizedCode

Используй стандартные фреймворки тестирования. Только код тестов."""

        // Добавляем логирование для отладки
        println("🔍 createTestGenerationPrompt:")
        println("   Extension: $extension")
        println("   Language: $language")
        println("   Original code length: ${sourceCode.length}")
        println("   Summarized code length: ${summarizedCode.length}")
        println("   Compression ratio: ${String.format("%.1f", (1.0 - summarizedCode.length.toDouble() / sourceCode.length) * 100)}%")

        return prompt
    }

    /**
     * Суммаризует код для генерации тестов, уменьшая размер запроса
     */
    private fun summarizeCodeForTesting(code: String, language: String): String {
        val lines = code.lines()
        val result = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Пропускаем пустые строки
            if (trimmed.isEmpty()) continue

            // Пропускаем комментарии
            when (language) {
                "Kotlin", "Java" -> {
                    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) continue
                }
                "Python" -> {
                    if (trimmed.startsWith("#")) continue
                }
                "JavaScript", "TypeScript" -> {
                    if (trimmed.startsWith("//") || trimmed.startsWith("/*")) continue
                }
            }

            // Упрощаем длинные строки
            val processedLine = if (line.length > 100) {
                line.substring(0, 97) + "..."
            } else {
                line
            }

            result.add(processedLine)
        }

        // Если код все еще слишком большой, используем сигнатуры функций
        val summarized = result.joinToString("\n")
        return if (summarized.length > 2000) {
            extractFunctionSignatures(code, language)
        } else {
            summarized
        }
    }

    /**
     * Извлекает только сигнатуры функций для генерации тестов
     */
    private fun extractFunctionSignatures(code: String, language: String): String {
        val lines = code.lines()
        val signatures = mutableListOf<String>()

        when (language) {
            "Kotlin" -> {
                for (line in lines) {
                    val trimmed = line.trim()
                    // Ищем функции Kotlin
                    if (trimmed.contains("fun ") && trimmed.contains("(") && trimmed.contains(")")) {
                        signatures.add("// Function signature: $trimmed")
                    }
                    // Ищем классы
                    if (trimmed.startsWith("class ") || trimmed.startsWith("interface ")) {
                        signatures.add("// $trimmed")
                    }
                }
            }
            "Java" -> {
                for (line in lines) {
                    val trimmed = line.trim()
                    // Ищем методы Java
                    if ((trimmed.contains("public") || trimmed.contains("private") || trimmed.contains("protected")) &&
                        (trimmed.contains("(") && trimmed.contains(")"))) {
                        signatures.add("// Method signature: $trimmed")
                    }
                    // Ищем классы
                    if (trimmed.startsWith("class ") || trimmed.startsWith("interface ")) {
                        signatures.add("// $trimmed")
                    }
                }
            }
            else -> {
                // Для других языков возвращаем первые 10 строк
                return lines.take(10).joinToString("\n")
            }
        }

        return if (signatures.isNotEmpty()) {
            signatures.joinToString("\n")
        } else {
            // Если не нашли сигнатуры, возвращаем первые 20 строк
            lines.take(20).joinToString("\n")
        }
    }
    
    /**
     * Генерирует тесты с помощью собственной логики (без AI)
     */
    suspend fun generateTestsManually(sourceCode: String, fileName: String, onMessage: ((String) -> Unit)? = null): String {
        val functions = extractFunctions(sourceCode)
        val allTests = mutableListOf<String>()

        onMessage?.invoke("📊 **ГЕНЕРИРУЮ ТЕСТЫ ПО ЧАСТЯМ:** Найдено ${functions.size} функций")

        for ((index, function) in functions.withIndex()) {
            onMessage?.invoke("🧪 **СОЗДАЮ ТЕСТЫ ДЛЯ ФУНКЦИИ ${index + 1}/${functions.size}:** ${function.name}")
            println("🔧 Function ${index + 1}: ${function.name}")
            println("   Signature: ${function.signature}")

            // Генерируем тесты с помощью собственной логики
            val testCode = generateManualTest(function)
            allTests.add(testCode)
            onMessage?.invoke("✅ **ТЕСТЫ ДЛЯ ФУНКЦИИ ${function.name} СОЗДАНЫ**")
        }

        // Создаем финальный файл
        val finalTestCode = createFinalTestFile(allTests, fileName, functions)

        val testMessage = """
🧪 **СГЕНЕРИРОВАННЫЕ ТЕСТЫ ПО ЧАСТЯМ:**
${"=".repeat(60)}
$finalTestCode
${"=".repeat(60)}
        """.trimIndent()
        onMessage?.invoke(testMessage)

        return finalTestCode
    }

    /**
     * Генерирует тест для функции с помощью собственной логики
     */
    private fun generateManualTest(function: FunctionInfo): String {
        val functionName = function.name
        val signature = function.signature
        val capitalizedName = functionName.replaceFirstChar { it.uppercase() }

        val testBody = when {
            // Тест для функции divide(a: Double, b: Double?)
            signature.contains("Double") && signature.contains("Double?") -> """
    @Test
    fun test${capitalizedName}() {
        val calculator = SimpleCalculator()

        // Тест с валидными значениями
        val result = calculator.${functionName}(10.0, 2.0)
        assertEquals(5.0, result, 0.0)
    }

    @Test
    fun test${capitalizedName}WithZero() {
        val calculator = SimpleCalculator()

        val result = calculator.${functionName}(10.0, 0.0)
        assertEquals(Double.POSITIVE_INFINITY, result, 0.0)
    }

    @Test(expected = NullPointerException::class)
    fun test${capitalizedName}WithNull() {
        val calculator = SimpleCalculator()

        calculator.${functionName}(10.0, null)
    }
            """.trimIndent()

            // Тест для функции add(a: Int, b: Int)
            signature.contains("Int") && signature.contains("Int)") && !signature.contains("?") -> """
    @Test
    fun test${capitalizedName}() {
        val calculator = SimpleCalculator()

        // Базовый тест
        val result = calculator.${functionName}(5, 3)
        assertEquals(8, result)
    }

    @Test
    fun test${capitalizedName}WithZero() {
        val calculator = SimpleCalculator()

        val result = calculator.${functionName}(0, 0)
        assertEquals(0, result)
    }

    @Test
    fun test${capitalizedName}WithNegative() {
        val calculator = SimpleCalculator()

        val result = calculator.${functionName}(-2, 3)
        assertEquals(1, result)
    }
            """.trimIndent()

            // Тест для функции processString(str: String?)
            signature.contains("String?") -> """
    @Test
    fun test${capitalizedName}() {
        val calculator = SimpleCalculator()

        // Тест с null
        val result1 = calculator.${functionName}(null)
        assertEquals("empty", result1)
    }

    @Test
    fun test${capitalizedName}WithValue() {
        val calculator = SimpleCalculator()

        // Тест с обычной строкой
        val result2 = calculator.${functionName}("hello")
        assertNotNull(result2)
    }

    @Test
    fun test${capitalizedName}WithSpaces() {
        val calculator = SimpleCalculator()

        // Тест со строкой с пробелами
        val result3 = calculator.${functionName}("  hello  ")
        assertEquals("hello", result3)
    }
            """.trimIndent()

            // Тест для функции countToFive()
            signature.contains("List<Int>") -> """
    @Test
    fun test${capitalizedName}() {
        val calculator = SimpleCalculator()

        val result = calculator.${functionName}()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(5, result.size)

        // Проверяем содержимое списка
        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }
            """.trimIndent()

            // Общий случай
            else -> """
    @Test
    fun test${capitalizedName}() {
        val calculator = SimpleCalculator()

        // Базовый тест для функции ${functionName}
        // TODO: Дополнить тест в соответствии с логикой функции
        // Сигнатура: $signature

        assertNotNull(calculator)
    }
            """.trimIndent()
        }

        return testBody
    }

    /**
     * Создает финальный тестовый файл
     */
    private fun createFinalTestFile(tests: List<String>, fileName: String, functions: List<FunctionInfo>): String {
        val testBody = tests.joinToString("\n\n")

        return """
package com.example

import org.junit.Test
import org.junit.Assert.*

class ${fileName.substringBeforeLast(".")}Test {

$testBody
}
        """.trimIndent()
    }

    /**
     * Извлекает функции из кода для генерации тестов по частям
     */
    private fun extractFunctions(code: String): List<FunctionInfo> {
        val lines = code.lines()
        val functions = mutableListOf<FunctionInfo>()

        var currentFunction: FunctionInfo? = null
        var braceCount = 0
        var initialBraceCount = 0

        println("🔍 Starting function extraction...")
        println("   Total lines: ${lines.size}")

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            // Отладочный вывод для каждой строки (только в режиме отладки)
            if (trimmed.contains("fun ")) {
                println("   🔍 Line ${index + 1} contains 'fun ': '$line'")
            }

            // Ищем начало функции - более простая логика
            if (currentFunction == null && line.contains("fun ")) {
                // Ищем позицию fun
                val funIndex = line.indexOf("fun ")
                if (funIndex != -1) {
                    // Ищем позицию открывающей скобки после fun
                    val parenStart = line.indexOf('(', funIndex)
                    if (parenStart != -1) {
                        // Ищем позицию закрывающей скобки
                        val parenEnd = line.indexOf(')', parenStart)
                        if (parenEnd != -1) {
                            // Вырезаем сигнатуру функции
                            val signature = line.substring(funIndex, parenEnd + 1).trim()
                            val functionName = extractFunctionName(signature)
                            println("   📍 Found function at line ${index + 1}: $functionName")
                            println("   📝 Signature: '$signature'")

                            currentFunction = FunctionInfo(
                                name = functionName,
                                signature = signature,
                                body = mutableListOf()
                            )
                            initialBraceCount = countBraces(line)
                            braceCount = initialBraceCount
                            println("   📊 Initial brace count: $initialBraceCount")
                        } else {
                            println("   ⚠️  No closing parenthesis found after '(' at position $parenStart")
                        }
                    } else {
                        println("   ⚠️  No opening parenthesis found after 'fun '")
                    }
                }
            }

            // Если находимся внутри функции
            if (currentFunction != null) {
                currentFunction.body.add(line)
                val lineBraceCount = countBraces(line)
                braceCount += lineBraceCount

                if (lineBraceCount != 0) {
                    println("   📊 Line ${index + 1} brace change: $lineBraceCount, total: $braceCount")
                }

                // Если функция закончилась (braceCount вернулся к начальному значению)
                if (braceCount == initialBraceCount && currentFunction.body.size > 1) {
                    functions.add(currentFunction)
                    println("   ✅ Function ${currentFunction.name} completed with ${currentFunction.body.size} lines")
                    currentFunction = null
                    braceCount = 0
                    initialBraceCount = 0
                }
            }
        }

        // Если последняя функция не закончилась, все равно добавляем ее
        if (currentFunction != null) {
            functions.add(currentFunction)
            println("   ✅ Last function ${currentFunction.name} added (was not properly closed)")
        }

        println("   📋 Total functions found: ${functions.size}")
        functions.forEach { func ->
            println("     - ${func.name}: ${func.body.size} lines")
        }

        return functions
    }

    /**
     * Извлекает имя функции из сигнатуры
     */
    private fun extractFunctionName(signature: String): String {
        val funIndex = signature.indexOf("fun ")
        if (funIndex == -1) return "unknown"

        val afterFun = signature.substring(funIndex + 4)
        val spaceIndex = afterFun.indexOf(' ')
        val parenIndex = afterFun.indexOf('(')

        return when {
            spaceIndex != -1 && spaceIndex < parenIndex -> afterFun.substring(0, spaceIndex)
            parenIndex != -1 -> afterFun.substring(0, parenIndex)
            else -> "unknown"
        }
    }

    /**
     * Считает количество открывающих и закрывающих скобок
     */
    private fun countBraces(line: String): Int {
        var count = 0
        for (char in line) {
            when (char) {
                '{' -> count++
                '}' -> count--
            }
        }
        return count
    }

    /**
     * Информация о функции
     */
    data class FunctionInfo(
        val name: String,
        val signature: String,
        val body: MutableList<String>
    )
    
    /**
     * Генерирует промпт для анализа результатов тестов
     */
    suspend fun analyzeTestResults(testOutput: String, originalCode: String): String {
        val prompt = """Проанализируй результаты выполнения тестов и создай подробный отчет.

Результаты тестов:
$testOutput

Исходный код:
$originalCode

Создай отчет, который включает:
1. Общее количество тестов
2. Количество пройденных/проваленных тестов
3. Анализ ошибок (если есть)
4. Рекомендации по улучшению кода
5. Общую оценку качества тестирования

Отвечай на русском языке."""

        val request = OpenAIRequest(
            model = "gpt-5",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = "Ты - эксперт по анализу результатов тестирования. Создавай подробные и понятные отчеты."
                ),
                OpenAIMessage(
                    role = "user",
                    content = prompt
                )
            ),
            maxCompletionTokens = 1500
        )
        
        return try {
            val response = openAIApi.chatCompletion(request)
            val content = response.choices.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                throw com.tayrinn.aiadvent.data.api.AIModelFailureException("Модель AI не смогла проанализировать код и вернула пустой ответ. Возможно, код слишком сложный.")
            }

            content
        } catch (e: Exception) {
            "Ошибка анализа результатов: ${e.message}"
        }
    }

    /**
     * Очищает ответ AI от markdown разметки и лишнего форматирования
     */
    private fun cleanTestResponse(response: String): String {
        var cleaned = response.trim()

        // Убираем блоки кода markdown
        cleaned = cleaned.replace(Regex("```\\w*\\s*"), "")
        cleaned = cleaned.replace(Regex("```\\s*"), "")

        // Убираем "kotlin" из начала, если есть
        if (cleaned.startsWith("kotlin\n")) {
            cleaned = cleaned.substring(7)
        }

        val lines = cleaned.lines().toMutableList()

        // Убираем пустые строки в начале
        while (lines.isNotEmpty() && lines.first().trim().isEmpty()) {
            lines.removeAt(0)
        }

        // Убираем пустые строки в конце
        while (lines.isNotEmpty() && lines.last().trim().isEmpty()) {
            lines.removeAt(lines.lastIndex)
        }

        // Обрабатываем каждую строку
        val processedLines = lines.map { line ->
            val trimmed = line.trim()

            // Пропускаем объявления package и class (они будут в основном файле)
            if (trimmed.startsWith("package ")) {
                return@map null
            }

            // Убираем только определенные импорты JUnit, но оставляем остальные
            if (trimmed.startsWith("import org.junit.jupiter.api.Test") ||
                trimmed.startsWith("import org.junit.jupiter.api.Assertions.*") ||
                (trimmed.startsWith("import ") && trimmed.contains("junit") && trimmed.contains("class "))) {
                return@map null
            }

            // Убираем объявления class только если они содержат "Test"
            if (trimmed.startsWith("class ") && trimmed.contains("Test")) {
                return@map null
            }

            // Убираем только очень короткие комментарии
            if (trimmed.startsWith("//") && trimmed.length < 15) {
                return@map null
            }

            // Очищаем строку от лишних пробелов, но сохраняем структуру
            line.trimEnd()
        }.filterNotNull()

        cleaned = processedLines.joinToString("\n")

        // Убираем лишние пустые строки (более 2 подряд)
        cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")

        // Убираем пробелы в начале файла
        cleaned = cleaned.trimStart()

        return cleaned
    }

    /**
     * Создает простой шаблонный тест на основе анализа сигнатуры функции
     */
    private fun createSimpleTestTemplate(functionName: String, signature: String): String {
        val capitalizedName = functionName.replaceFirstChar { it.uppercase() }

        // Анализируем сигнатуру для создания более умного шаблона
        val testBody = when {
            signature.contains("Double") && signature.contains("Double?") -> {
                """
        val calculator = SimpleCalculator()

        // Тест с валидными значениями
        val result = calculator.$functionName(10.0, 2.0)
        assertEquals(5.0, result, 0.0)

        // Тест с null значением
        try {
            calculator.$functionName(10.0, null)
            fail("Expected NullPointerException")
        } catch (e: NullPointerException) {
            // Ожидаемое исключение
        }
                """.trimIndent()
            }
            signature.contains("Int") && signature.contains("Int)") -> {
                """
        val calculator = SimpleCalculator()

        // Базовый тест
        val result = calculator.$functionName(5, 3)
        assertEquals(8, result) // Проверить ожидаемое значение

        // Дополнительный тест
        val result2 = calculator.$functionName(0, 0)
        assertEquals(0, result2)
                """.trimIndent()
            }
            signature.contains("String?") -> {
                """
        val calculator = SimpleCalculator()

        // Тест с null
        val result1 = calculator.$functionName(null)
        assertEquals("empty", result1)

        // Тест с валидной строкой
        val result2 = calculator.$functionName("test")
        assertNotNull(result2)
                """.trimIndent()
            }
            signature.contains("List<Int>") -> {
                """
        val calculator = SimpleCalculator()

        val result = calculator.$functionName()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        // Проверить содержимое списка
        // assertEquals(expectedList, result)
                """.trimIndent()
            }
            else -> {
                """
        val calculator = SimpleCalculator()

        // TODO: Дополнить тест в соответствии с логикой функции
        // Сигнатура: $signature

        // val result = calculator.$functionName(/* параметры */)
        // assertEquals(/* ожидаемое значение */, result)
                """.trimIndent()
            }
        }

        return """
    @Test
    fun test$capitalizedName() {
$testBody
    }
        """.trimIndent()
    }

    /**
     * Запускает самотестирование системы генерации тестов
     */
    fun runSelfTests(): String {
        val results = mutableListOf<String>()

        // Тест 0: Проверка реального файла SimpleTestFile.kt
        results.add("🧪 **ТЕСТ 0: Парсинг реального файла SimpleTestFile.kt**")
        try {
            val realFileDebug = debugFileParsing("SimpleTestFile.kt")
            results.add(realFileDebug)
        } catch (e: Exception) {
            results.add("❌ Ошибка тестирования реального файла: ${e.message}")
        }
        results.add("")

        // Тест 0.5: Проверка парсинга с жестко закодированным содержимым
        results.add("🧪 **ТЕСТ 0.5: Парсинг с жестко закодированным содержимым**")
        val hardcodedContent = """
package com.example

class SimpleCalculator {

    fun divide(a: Double, b: Double?): Double {
        return a / b!!
    }

    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun processString(str: String?): String {
        if (str == null) {
            return "empty"
        }
        return str
    }

    fun countToFive(): List<Int> {
        val result = mutableListOf<Int>()
        var i = 1
        while (i <= 5) {
            result.add(i)
        }
        return result
    }
}
        """.trimIndent()

        results.add("   Жестко закодированный код:")
        hardcodedContent.lines().forEachIndexed { index, line ->
            results.add("   ${String.format("%2d", index + 1)}: $line")
        }
        results.add("")

        val hardcodedFunctions = extractFunctions(hardcodedContent)
        results.add("   Найдено функций: ${hardcodedFunctions.size}")
        hardcodedFunctions.forEach { func ->
            results.add("   ✅ ${func.name}: ${func.signature}")
        }

        if (hardcodedFunctions.size != 4) {
            results.add("   ❌ ОШИБКА: Ожидалось 4 функции, найдено ${hardcodedFunctions.size}")
        } else {
            results.add("   ✅ Парсинг с жестко закодированным кодом работает!")
        }
        results.add("")

        // Тест 1: Проверка парсинга функций с отступами (как в реальном файле)
        val testCode = """
package com.example

class SimpleCalculator {

    fun divide(a: Double, b: Double?): Double {
        return a / b!!
    }

    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun processString(str: String?): String {
        return str ?: "empty"
    }

    fun countToFive(): List<Int> {
        return listOf(1, 2, 3, 4, 5)
    }
}
        """.trimIndent()

        results.add("🧪 **ТЕСТ 1: Парсинг функций с отступами**")
        results.add("   Тестовый код:")
        testCode.lines().forEachIndexed { index, line ->
            results.add("   ${String.format("%2d", index + 1)}: $line")
        }
        results.add("")

        val functions = extractFunctions(testCode)
        results.add("   Найдено функций: ${functions.size}")
        functions.forEach { func ->
            results.add("   ✅ ${func.name}: ${func.signature}")
        }

        if (functions.size != 4) {
            results.add("   ❌ ОШИБКА: Ожидалось 4 функции, найдено ${functions.size}")
        } else {
            results.add("   ✅ Тест пройден")
        }

        // Тест 2: Проверка генерации тестов
        results.add("\n🧪 **ТЕСТ 2: Генерация тестов**")
        val testFunction = FunctionInfo("divide", "fun divide(a: Double, b: Double?): Double", mutableListOf())
        val generatedTest = generateManualTest(testFunction)

        results.add("   Сгенерированный тест содержит @Test: ${generatedTest.contains("@Test")}")
        results.add("   Сгенерированный тест содержит assertEquals: ${generatedTest.contains("assertEquals")}")
        results.add("   Сгенерированный тест содержит assertThrows: ${generatedTest.contains("assertThrows")}")

        if (generatedTest.contains("@Test") && generatedTest.contains("assertEquals") && generatedTest.contains("assertThrows")) {
            results.add("   ✅ Тест пройден")
        } else {
            results.add("   ❌ ОШИБКА: Сгенерированный тест не содержит необходимых элементов")
        }

        // Тест 3: Проверка финального файла
        results.add("\n🧪 **ТЕСТ 3: Финальный файл**")
        val finalFile = createFinalTestFile(listOf(generatedTest), "Test", functions)

        results.add("   Финальный файл содержит package: ${finalFile.contains("package com.example")}")
        results.add("   Финальный файл содержит imports: ${finalFile.contains("import org.junit")}")
        results.add("   Финальный файл содержит class: ${finalFile.contains("class TestTest")}")
        results.add("   Финальный файл содержит @Test: ${finalFile.contains("@Test")}")

        if (finalFile.contains("package com.example") &&
            finalFile.contains("import org.junit") &&
            finalFile.contains("class TestTest") &&
            finalFile.contains("@Test")) {
            results.add("   ✅ Тест пройден")
        } else {
            results.add("   ❌ ОШИБКА: Финальный файл не содержит необходимых элементов")
        }

        return results.joinToString("\n")
    }

    companion object {
        /**
         * Статическая версия extractFunctions для тестирования
         */
        @JvmStatic
        fun extractFunctionsStatic(code: String): List<FunctionInfo> {
            val lines = code.lines()
            val functions = mutableListOf<FunctionInfo>()

            var currentFunction: FunctionInfo? = null
            var braceCount = 0
            var initialBraceCount = 0

            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()

                // Ищем начало функции - более простая логика
                if (currentFunction == null && line.contains("fun ")) {
                    // Ищем позицию fun
                    val funIndex = line.indexOf("fun ")
                    if (funIndex != -1) {
                        // Ищем позицию открывающей скобки после fun
                        val parenStart = line.indexOf('(', funIndex)
                        if (parenStart != -1) {
                            // Ищем позицию закрывающей скобки
                            val parenEnd = line.indexOf(')', parenStart)
                            if (parenEnd != -1) {
                                // Вырезаем сигнатуру функции
                                val signature = line.substring(funIndex, parenEnd + 1).trim()
                                val functionName = extractFunctionNameStatic(signature)
                                println("   📍 Found function at line ${index + 1}: $functionName")
                                println("   📝 Signature: '$signature'")

                                currentFunction = FunctionInfo(
                                    name = functionName,
                                    signature = signature,
                                    body = mutableListOf()
                                )
                                initialBraceCount = countBracesStatic(line)
                                braceCount = initialBraceCount
                            }
                        }
                    }
                }

                // Если находимся внутри функции
                if (currentFunction != null) {
                    currentFunction.body.add(line)
                    val lineBraceCount = countBracesStatic(line)
                    braceCount += lineBraceCount

                    // Если функция закончилась (braceCount вернулся к начальному значению)
                    if (braceCount == initialBraceCount && currentFunction.body.size > 1) {
                        functions.add(currentFunction)
                        println("   ✅ Function ${currentFunction.name} completed with ${currentFunction.body.size} lines")
                        currentFunction = null
                        braceCount = 0
                        initialBraceCount = 0
                    }
                }
            }

            // Если последняя функция не закончилась, все равно добавляем ее
            if (currentFunction != null) {
                functions.add(currentFunction)
                println("   ✅ Last function ${currentFunction.name} added (was not properly closed)")
            }

            return functions
        }

        /**
         * Статическая версия extractFunctionName для тестирования
         */
        @JvmStatic
        fun extractFunctionNameStatic(signature: String): String {
            val funIndex = signature.indexOf("fun ")
            if (funIndex == -1) return "unknown"

            val afterFun = signature.substring(funIndex + 4)
            val spaceIndex = afterFun.indexOf(' ')
            val parenIndex = afterFun.indexOf('(')

            return when {
                spaceIndex != -1 && spaceIndex < parenIndex -> afterFun.substring(0, spaceIndex)
                parenIndex != -1 -> afterFun.substring(0, parenIndex)
                else -> "unknown"
            }
        }

        /**
         * Статическая версия countBraces для тестирования
         */
        @JvmStatic
        private fun countBracesStatic(line: String): Int {
            var count = 0
            for (char in line) {
                when (char) {
                    '{' -> count++
                    '}' -> count--
                }
            }
            return count
        }

        /**
         * Статическая версия debugFileParsing для тестирования
         */
        @JvmStatic
        fun debugFileParsingStatic(filePath: String): String {
            val results = mutableListOf<String>()

            try {
                val fileService = com.tayrinn.aiadvent.service.FileService()
                // Попробуем разные пути к файлу
                val possiblePaths = listOf(
                    filePath,
                    "/Users/tayrinn/AndroidStudioProjects/AIAdvent/$filePath",
                    "./$filePath",
                    System.getProperty("user.dir") + "/$filePath"
                )

                var sourceCode = ""
                var actualPath = ""

                results.add("🔍 **ПОИСК ФАЙЛА:**")
                results.add("   Текущая директория: ${System.getProperty("user.dir")}")
                results.add("   Искомый файл: $filePath")
                results.add("")

                for (path in possiblePaths) {
                    try {
                        results.add("   🔍 Проверяем: $path")
                        sourceCode = fileService.readFile(path)
                        if (sourceCode.startsWith("Ошибка чтения файла")) {
                            results.add("   ❌ Ошибка: $sourceCode")
                        } else {
                            actualPath = path
                            results.add("   ✅ Файл найден: $path (${sourceCode.length} символов)")
                            break
                        }
                    } catch (e: Exception) {
                        results.add("   ❌ Исключение: ${e.message}")
                    }
                }

                if (sourceCode.isEmpty() || sourceCode.startsWith("Ошибка чтения файла")) {
                    results.add("❌ Файл не найден ни по одному пути!")
                    return results.joinToString("\n")
                }

                results.add("🔍 **ОТЛАДКА ПАРСИНГА ФАЙЛА: $filePath**")
                results.add("   Размер файла: ${sourceCode.length} символов")
                results.add("")

                results.add("📋 **СОДЕРЖИМОЕ ФАЙЛА:**")
                sourceCode.lines().forEachIndexed { index, line ->
                    results.add("   ${String.format("%2d", index + 1)}: $line")
                }
                results.add("")

                val functions = extractFunctionsStatic(sourceCode)
                results.add("📊 **РЕЗУЛЬТАТЫ ПАРСИНГА:**")
                results.add("   Найдено функций: ${functions.size}")

                if (functions.isEmpty()) {
                    results.add("   ⚠️  Функции не найдены!")
                    results.add("")
                    results.add("🔍 **ПРОВЕРКА СТРОК С 'fun ':**")
                    sourceCode.lines().forEachIndexed { index, line ->
                        if (line.contains("fun ")) {
                            results.add("   📍 Строка ${index + 1}: '$line'")
                            // Проверим, есть ли скобки
                            val funIndex = line.indexOf("fun ")
                            val parenStart = line.indexOf('(', funIndex)
                            val parenEnd = line.indexOf(')', parenStart)
                            results.add("      funIndex=$funIndex, parenStart=$parenStart, parenEnd=$parenEnd")
                        }
                    }
                } else {
                    functions.forEach { func ->
                        results.add("   ✅ ${func.name}: ${func.signature} (${func.body.size} строк)")
                    }
                }

            } catch (e: Exception) {
                results.add("❌ Ошибка при отладке файла: ${e.message}")
            }

            return results.joinToString("\n")
        }

        /**
         * Тестовая функция main для проверки парсинга
         */
        @JvmStatic
        fun main(args: Array<String>) {
        println("🚀 ЗАПУСК ТЕСТА ПАРСИНГА ФУНКЦИЙ")
        println("=================================")

        // Тест с жестко закодированным содержимым
        val hardcodedContent = """
package com.example

class SimpleCalculator {

    fun divide(a: Double, b: Double?): Double {
        return a / b!!
    }

    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun processString(str: String?): String {
        if (str == null) {
            return "empty"
        }
        return str
    }

    fun countToFive(): List<Int> {
        val result = mutableListOf<Int>()
        var i = 1
        while (i <= 5) {
            result.add(i)
        }
        return result
    }
}
        """.trimIndent()

        println("🧪 ТЕСТ 1: Парсинг с жестко закодированным содержимым")
        println("   Содержимое:")
        hardcodedContent.lines().forEachIndexed { index, line ->
            println("   ${String.format("%2d", index + 1)}: $line")
        }
        println()

        // Создаем тестовый сервис без зависимостей для тестирования парсинга
        val testService = object {
            fun extractFunctions(code: String): List<FunctionInfo> {
                return TestGenerationService.extractFunctionsStatic(code)
            }
            fun extractFunctionName(signature: String): String {
                return TestGenerationService.extractFunctionNameStatic(signature)
            }
            fun debugFileParsing(filePath: String): String {
                return TestGenerationService.debugFileParsingStatic(filePath)
            }
        }
        val functions = testService.extractFunctions(hardcodedContent)
        println("   Результаты парсинга:")
        println("   Найдено функций: ${functions.size}")

        if (functions.isEmpty()) {
            println("   ❌ ПРОБЛЕМА: Функции не найдены!")
            println("\n🔍 Диагностика поиска:")

            hardcodedContent.lines().forEachIndexed { index, line ->
                if (line.contains("fun ")) {
                    val funIndex = line.indexOf("fun ")
                    val parenStart = line.indexOf('(', funIndex)
                    val parenEnd = line.indexOf(')', parenStart)
                    println("   Строка ${index + 1}: '$line'")
                    println("      funIndex=$funIndex, parenStart=$parenStart, parenEnd=$parenEnd")

                    if (parenEnd != -1) {
                        val signature = line.substring(funIndex, parenEnd + 1).trim()
                        println("      Сигнатура: '$signature'")

                        val functionName = testService.extractFunctionName(signature)
                        println("      Имя функции: '$functionName'")
                    }
                    println()
                }
            }
        } else {
            functions.forEachIndexed { _, func ->
                println("   ✅ ${func.name}: ${func.signature} (${func.body.size} строк)")
            }
        }

        println("\n" + "=".repeat(50))

        // Тест с реальным файлом
        println("\n🧪 ТЕСТ 2: Парсинг реального файла SimpleTestFile.kt")
        val debugResult = testService.debugFileParsing("SimpleTestFile.kt")
        println(debugResult)

        println("\n🎉 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО")
        }
    }

    /**
     * Отладочная функция для проверки парсинга конкретного файла
     */
    fun debugFileParsing(filePath: String): String {
        val results = mutableListOf<String>()

        try {
            val fileService = com.tayrinn.aiadvent.service.FileService()
            // Попробуем разные пути к файлу
            val possiblePaths = listOf(
                filePath,
                "/Users/tayrinn/AndroidStudioProjects/AIAdvent/$filePath",
                "./$filePath",
                System.getProperty("user.dir") + "/$filePath"
            )

            var sourceCode = ""
            var actualPath = ""

            results.add("🔍 **ПОИСК ФАЙЛА:**")
            results.add("   Текущая директория: ${System.getProperty("user.dir")}")
            results.add("   Искомый файл: $filePath")
            results.add("")

            for (path in possiblePaths) {
                try {
                    results.add("   🔍 Проверяем: $path")
                    sourceCode = fileService.readFile(path)
                    if (sourceCode.startsWith("Ошибка чтения файла")) {
                        results.add("   ❌ Ошибка: $sourceCode")
                    } else {
                        actualPath = path
                        results.add("   ✅ Файл найден: $path (${sourceCode.length} символов)")
                        break
                    }
                } catch (e: Exception) {
                    results.add("   ❌ Исключение: ${e.message}")
                }
            }

            if (sourceCode.isEmpty() || sourceCode.startsWith("Ошибка чтения файла")) {
                results.add("❌ Файл не найден ни по одному пути!")
                return results.joinToString("\n")
            }

            results.add("🔍 **ОТЛАДКА ПАРСИНГА ФАЙЛА: $filePath**")
            results.add("   Размер файла: ${sourceCode.length} символов")
            results.add("")

            results.add("📋 **СОДЕРЖИМОЕ ФАЙЛА:**")
            sourceCode.lines().forEachIndexed { index, line ->
                results.add("   ${String.format("%2d", index + 1)}: $line")
            }
            results.add("")

            val functions = extractFunctions(sourceCode)
            results.add("📊 **РЕЗУЛЬТАТЫ ПАРСИНГА:**")
            results.add("   Найдено функций: ${functions.size}")

            if (functions.isEmpty()) {
                results.add("   ⚠️  Функции не найдены!")
                results.add("")
                results.add("🔍 **ПРОВЕРКА СТРОК С 'fun ':**")
                sourceCode.lines().forEachIndexed { index, line ->
                    if (line.contains("fun ")) {
                        results.add("   📍 Строка ${index + 1}: '$line'")
                        // Проверим, есть ли скобки
                        val funIndex = line.indexOf("fun ")
                        val parenStart = line.indexOf('(', funIndex)
                        val parenEnd = line.indexOf(')', parenStart)
                        results.add("      funIndex=$funIndex, parenStart=$parenStart, parenEnd=$parenEnd")
                    }
                }
            } else {
                functions.forEach { func ->
                    results.add("   ✅ ${func.name}: ${func.signature} (${func.body.size} строк)")
                }
            }

        } catch (e: Exception) {
            results.add("❌ Ошибка при отладке файла: ${e.message}")
        }

        return results.joinToString("\n")
    }
}
