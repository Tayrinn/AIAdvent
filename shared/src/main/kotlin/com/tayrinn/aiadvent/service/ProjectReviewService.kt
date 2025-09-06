package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.createOpenAIApiImpl
import com.tayrinn.aiadvent.data.model.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

data class ProjectStructure(
    val rootPath: String,
    val files: List<ProjectFile>,
    val totalFiles: Int,
    val totalLines: Int,
    val languages: Map<String, Int> // язык -> количество файлов
)

data class ProjectFile(
    val relativePath: String,
    val absolutePath: String,
    val size: Long,
    val extension: String,
    val language: String,
    val lines: Int = 0
)

data class ReviewSession(
    val projectStructure: ProjectStructure,
    val currentStage: ReviewStage,
    val reviewedFiles: MutableSet<String> = mutableSetOf(),
    val findings: MutableList<ReviewFinding> = mutableListOf()
)

enum class ReviewStage {
    INITIAL_ANALYSIS,    // Первичный анализ структуры
    DETAILED_REVIEW,     // Детальный анализ файлов
    FINAL_RECOMMENDATIONS // Финальные рекомендации
}

data class ReviewFinding(
    val type: FindingType,
    val file: String,
    val description: String,
    val recommendation: String,
    val severity: Severity
)

enum class FindingType {
    CODE_DUPLICATION,
    PERFORMANCE_ISSUE,
    SECURITY_ISSUE,
    READABILITY,
    OUTDATED_DEPENDENCY,
    ARCHITECTURE,
    BEST_PRACTICES
}

enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

class ProjectReviewService {
    
    private val openAIApi = createOpenAIApiImpl()
    private val json = Json { ignoreUnknownKeys = true }
    private var currentSession: ReviewSession? = null
    private val httpClient by lazy { OkHttpClient() }
    
    // Расширения файлов, которые мы анализируем
    private val supportedExtensions = setOf(
        "kt", "java", "js", "ts", "py", "cpp", "c", "h", "cs", "go", "rs",
        "swift", "php", "rb", "scala", "clj", "hs", "ml", "r", "sql",
        "html", "css", "scss", "less", "xml", "json", "yaml", "yml",
        "gradle", "pom", "package", "requirements", "Dockerfile", "Makefile"
    )
    
    /**
     * Анализирует структуру проекта
     */
    suspend fun analyzeProject(projectPath: String): ProjectStructure = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(projectPath)
            if (!rootDir.exists() || !rootDir.isDirectory) {
                throw IllegalArgumentException("Путь должен указывать на существующую папку")
            }

            println("📁 Анализируем проект: $projectPath")
            println("🔎 Сканируем файлы и каталоги... Это может занять немного времени на больших проектах.")

            val files = mutableListOf<ProjectFile>()
            val languages = mutableMapOf<String, Int>()
            var totalLines = 0
            var scannedEntries = 0
            val maxEntriesToScan = 20000 // ограничение на количество просматриваемых путей, чтобы не зависать на огромных папках (например, venv)

            Files.walk(Paths.get(projectPath))
                .limit(maxEntriesToScan.toLong())
                .filter { Files.isRegularFile(it) }
                .filter { !isIgnoredPath(it.toString()) }
                .toList()
                .forEach { path ->
                    val file = path.toFile()
                    val extension = file.extension.lowercase()

                    if (supportedExtensions.contains(extension) || file.name.lowercase() in supportedExtensions) {
                        scannedEntries++
                        if (scannedEntries % 500 == 0) {
                            println("⏳ Просканировано файлов: $scannedEntries...")
                        }

                        // Пропускаем слишком большие файлы (> 2 МБ)
                        val fileSizeBytes = file.length()
                        if (fileSizeBytes > 2 * 1024 * 1024) {
                            return@forEach
                        }

                        val language = detectLanguage(extension, file.name)
                        val lines = countLines(file)

                        files.add(
                            ProjectFile(
                                relativePath = rootDir.toPath().relativize(path).toString(),
                                absolutePath = path.toString(),
                                size = file.length(),
                                extension = extension,
                                language = language,
                                lines = lines
                            )
                        )

                        languages[language] = languages.getOrDefault(language, 0) + 1
                        totalLines += lines
                    }
                }

            ProjectStructure(
                rootPath = projectPath,
                files = files.sortedBy { it.relativePath },
                totalFiles = files.size,
                totalLines = totalLines,
                languages = languages
            )
        } catch (e: Exception) {
            println("❌ Ошибка анализа проекта (analyzeProject): ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    @Serializable
    data class ReviewDecision(
        val advice: String,
        val next_command: String? = null
    )

    /**
     * На основе результата предыдущей команды запрашивает у ИИ советы и следующую команду
     */
    suspend fun decideNextAction(previousResult: String): ReviewDecision? {
        val session = currentSession ?: return null
        val structure = session.projectStructure

        val prompt = """
Ты выступаешь в роли строгого и эффективного ревьювера кода. Ты работаешь в интерактивном режиме по командам.

КОНТЕКСТ ПРОЕКТА:
Путь: ${structure.rootPath}
Всего файлов: ${structure.totalFiles}
Языки: ${structure.languages.entries.joinToString(", ") { it.key + ": " + it.value }}

РЕЗУЛЬТАТ ПРЕДЫДУЩЕЙ КОМАНДЫ:
"""
            .trimIndent() + "\n\n" + previousResult + "\n\n" +
            """
ТВОЯ ЗАДАЧА:
1) Дай краткие и практичные советы по улучшению по результатам (до 10 пунктов, по делу)
2) Выбери СЛЕДУЮЩУЮ КОМАНДУ из списка (строго одну):
   - "show file: <путь>"
   - "list files: <паттерн>"
   - "analyze dependencies"
   - "find duplicates"
   - "check architecture"
   - "final report"

ФОРМАТ ОТВЕТА: строго JSON, без пояснений и без Markdown.
{
  "advice": "краткие советы...",
  "next_command": "show file: src/..." | "analyze dependencies" | "find duplicates" | "check architecture" | "list files: .kt" | "final report" | null
}
""".trimIndent()

        val response = chatWithPreferredProvider(prompt)

        // Пытаемся вытащить чистый JSON из ответа (на случай если модель добавила текст)
        val jsonRegex = Regex("\\{[\\s\\S]*?\\}")
        val jsonStr = jsonRegex.find(response)?.value ?: response
        return try {
            json.decodeFromString<ReviewDecision>(jsonStr)
        } catch (e: Exception) {
            println("❌ Ошибка парсинга ReviewDecision JSON: ${e.message}")
            null
        }
    }

    /**
     * Генерация стартового анализа: предпочтительно через OpenAI (env.local), иначе через текущего провайдера
     */
    private suspend fun generateInitialAnalysis(structure: ProjectStructure): String {
        val prompt = """
Ты - ИИ-ревьювер кода. Твоя задача - проанализировать проект ПОШАГОВО, используя команды.

СТРУКТУРА ПРОЕКТА:
Путь: ${structure.rootPath}
Всего файлов: ${structure.totalFiles}
Всего строк кода: ${structure.totalLines}

ЯЗЫКИ ПРОГРАММИРОВАНИЯ:
${structure.languages.entries.joinToString("\n") { "- ${it.key}: ${it.value} файлов" }}

ФАЙЛЫ (первые 50):
${structure.files.take(50).joinToString("\n") { "- ${it.relativePath} (${it.lines} строк, ${it.language})" }}

ВАЖНО: НЕ анализируй проект самостоятельно! Используй команды для получения информации.

ДОСТУПНЫЕ КОМАНДЫ:
- "show file: <путь>" - показать содержимое файла
- "list files: <расширение>" - показать файлы определенного типа  
- "analyze dependencies" - анализ зависимостей
- "find duplicates" - поиск дублирования кода
- "check architecture" - проверка архитектуры
- "final report" - финальный отчёт

АЛГОРИТМ РАБОТЫ:
1. Сначала изучи зависимости: "analyze dependencies"
2. Посмотри на ключевые файлы: "show file: <путь к главному файлу>"
3. Найди дубликаты: "find duplicates"
4. Проверь архитектуру: "check architecture"
5. Сделай финальный отчёт: "final report"

Начни анализ с команды для изучения зависимостей. Пиши ТОЛЬКО команду, без дополнительных объяснений!
        """.trimIndent()

        return chatWithPreferredProvider(prompt)
    }

    /**
     * Отправляет чат-запрос: если в env.local настроен OpenAI, используем его; иначе fallback на существующий провайдер
     */
    private suspend fun chatWithPreferredProvider(userPrompt: String): String {
        val cfg = loadOpenAIConfig()
        return if (cfg != null) {
            val (apiKey, model) = cfg
            chatOpenAI(userPrompt, apiKey, model)
        } else {
            // fallback к существующему API
            val (resp, _) = openAIApi.sendMessage(userPrompt, emptyList(), null)
            println("🤖 AI response (fallback provider):\n$resp")
            resp
        }
    }

    // ===== OpenAI ChatGPT (env.local) =====

    @Serializable
    private data class OpenAIMessage(val role: String, val content: String)
    @Serializable
    private data class OpenAIChatRequest(val model: String, val messages: List<OpenAIMessage>, val temperature: Double = 0.3)
    @Serializable
    private data class OpenAIChoice(val index: Int, val message: OpenAIMessage)
    @Serializable
    private data class OpenAIChatResponse(val choices: List<OpenAIChoice>)

    private fun loadOpenAIConfig(): Pair<String, String>? {
        // Ищем env.local в текущем и родительских каталогах (до 3 уровней)
        val candidates = listOf(
            "env.local",
            "../env.local",
            "../../env.local",
            "../../../env.local"
        )
        val file = candidates.map { java.io.File(it) }.firstOrNull { it.exists() && it.isFile }
        if (file == null) return null
        val lines = file.readLines()
        val map = lines.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) null
            else {
                val idx = trimmed.indexOf('=')
                if (idx > 0) trimmed.substring(0, idx).trim() to trimmed.substring(idx + 1).trim() else null
            }
        }.toMap()
        val apiKey = map["OPENAI_API_KEY"] ?: map["OPENAI.KEY"]
        val model = map["OPENAI_MODEL"] ?: map["OPENAI.MODEL"] ?: "gpt-4o-mini"
        return if (!apiKey.isNullOrBlank()) apiKey to model else null
    }

    private suspend fun chatOpenAI(userPrompt: String, apiKey: String, model: String): String = withContext(Dispatchers.IO) {
        val url = "https://api.openai.com/v1/chat/completions"
        val systemMsg = OpenAIMessage(role = "system", content = "Ты - полезный AI помощник. Отвечай на русском, будь краток и по делу.")
        val userMsg = OpenAIMessage(role = "user", content = userPrompt)
        val reqObj = OpenAIChatRequest(model = model, messages = listOf(systemMsg, userMsg))
        val bodyStr = json.encodeToString(OpenAIChatRequest.serializer(), reqObj)
        val requestBody: RequestBody = bodyStr.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        httpClient.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                println("❌ OpenAI API Error (${resp.code}): $text")
                return@withContext "Извините, произошла ошибка при обращении к OpenAI API. Код ошибки: ${resp.code}"
            }
            val parsed = json.decodeFromString(OpenAIChatResponse.serializer(), text)
            val content = parsed.choices.firstOrNull()?.message?.content ?: ""
            println("🤖 OpenAI response:\n$content")
            return@withContext content
        }
    }

    /**
     * Начинает сессию ревью проекта
     */
    suspend fun startReviewSession(projectStructure: ProjectStructure): String {
        currentSession = ReviewSession(
            projectStructure = projectStructure,
            currentStage = ReviewStage.INITIAL_ANALYSIS
        )
        
        return generateInitialAnalysis(projectStructure)
    }
    
    /**
     * Извлекает команду из текста ответа ИИ
     */
    fun extractCommandFromResponse(response: String): String? {
        val supportedCommands = listOf(
            "show file:",
            "list files:",
            "analyze dependencies",
            "find duplicates", 
            "check architecture",
            "final report"
        )
        
        // Ищем команду в каждой строке
        response.lines().forEach { line ->
            val trimmedLine = line.trim()
            supportedCommands.forEach { command ->
                if (trimmedLine.startsWith(command, ignoreCase = true)) {
                    return trimmedLine
                }
            }
        }
        
        // Если не найдено в отдельных строках, ищем в тексте
        supportedCommands.forEach { command ->
            if (response.contains(command, ignoreCase = true)) {
                // Извлекаем строку с командой
                val lines = response.split('\n')
                val commandLine = lines.find { it.contains(command, ignoreCase = true) }
                if (commandLine != null) {
                    return commandLine.trim()
                }
            }
        }
        
        return null
    }

    /**
     * Обрабатывает команды от ИИ
     */
    suspend fun processCommand(command: String): String = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext "❌ Сессия ревью не начата"
        
        println("🔧 Обрабатываем команду: '$command'")
        
        return@withContext when {
            command.startsWith("show file:", ignoreCase = true) -> {
                val filePath = command.substringAfter("show file:").trim()
                showFileContent(filePath)
            }
            command.startsWith("list files:", ignoreCase = true) -> {
                val pattern = command.substringAfter("list files:").trim()
                listFiles(pattern)
            }
            command.startsWith("analyze dependencies", ignoreCase = true) -> {
                analyzeDependencies()
            }
            command.startsWith("find duplicates", ignoreCase = true) -> {
                findCodeDuplicates()
            }
            command.startsWith("check architecture", ignoreCase = true) -> {
                checkArchitecture()
            }
            command.startsWith("final report", ignoreCase = true) -> {
                generateFinalReport()
            }
            else -> {
                "❓ Неизвестная команда. Доступные команды:\n" +
                "- show file: <путь к файлу>\n" +
                "- list files: <паттерн или расширение>\n" +
                "- analyze dependencies\n" +
                "- find duplicates\n" +
                "- check architecture\n" +
                "- final report"
            }
        }
    }
    
    // (реализовано выше как chatWithPreferredProvider)
    
    private fun showFileContent(filePath: String): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        // Ищем файл в структуре проекта
        val projectFile = session.projectStructure.files.find { 
            it.relativePath.equals(filePath, ignoreCase = true) || 
            it.relativePath.endsWith(filePath, ignoreCase = true)
        }
        
        if (projectFile == null) {
            val suggestions = session.projectStructure.files
                .filter { it.relativePath.contains(filePath, ignoreCase = true) }
                .take(5)
                .joinToString("\n") { "- ${it.relativePath}" }
            
            return "❌ Файл '$filePath' не найден.\n" +
                if (suggestions.isNotEmpty()) "Возможно, вы имели в виду:\n$suggestions" else ""
        }
        
        return try {
            val file = File(projectFile.absolutePath)
            val content = file.readText()
            
            session.reviewedFiles.add(projectFile.relativePath)
            
            "📄 **${projectFile.relativePath}** (${projectFile.lines} строк, ${projectFile.language})\n\n" +
            "```${projectFile.language.lowercase()}\n$content\n```"
        } catch (e: Exception) {
            "❌ Ошибка чтения файла: ${e.message}"
        }
    }
    
    private fun listFiles(pattern: String): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        val filteredFiles = when {
            pattern.startsWith(".") -> {
                // Фильтр по расширению
                session.projectStructure.files.filter { it.extension.equals(pattern.drop(1), ignoreCase = true) }
            }
            else -> {
                // Фильтр по паттерну в пути
                session.projectStructure.files.filter { it.relativePath.contains(pattern, ignoreCase = true) }
            }
        }
        
        if (filteredFiles.isEmpty()) {
            return "❌ Файлы по паттерну '$pattern' не найдены"
        }
        
        val grouped = filteredFiles.groupBy { it.language }
        
        return buildString {
            appendLine("📋 **Найдено файлов: ${filteredFiles.size}**")
            appendLine()
            
            grouped.forEach { (language, files) ->
                appendLine("**$language (${files.size} файлов):**")
                files.take(20).forEach { file ->
                    appendLine("- ${file.relativePath} (${file.lines} строк)")
                }
                if (files.size > 20) {
                    appendLine("... и ещё ${files.size - 20} файлов")
                }
                appendLine()
            }
        }
    }
    
    private fun analyzeDependencies(): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        val dependencyFiles = session.projectStructure.files.filter { file ->
            file.relativePath.lowercase().let { path ->
                path.endsWith("package.json") ||
                path.endsWith("build.gradle") ||
                path.endsWith("build.gradle.kts") ||
                path.endsWith("pom.xml") ||
                path.endsWith("requirements.txt") ||
                path.endsWith("composer.json") ||
                path.endsWith("cargo.toml") ||
                path.endsWith("go.mod")
            }
        }
        
        if (dependencyFiles.isEmpty()) {
            return "❌ Файлы зависимостей не найдены"
        }
        
        return buildString {
            appendLine("📦 **Найденные файлы зависимостей:**")
            appendLine()
            
            dependencyFiles.forEach { file ->
                try {
                    val content = File(file.absolutePath).readText()
                    appendLine("**${file.relativePath}:**")
                    appendLine("```")
                    appendLine(content.take(1000) + if (content.length > 1000) "\n... (обрезано)" else "")
                    appendLine("```")
                    appendLine()
                } catch (e: Exception) {
                    appendLine("**${file.relativePath}:** ❌ Ошибка чтения")
                }
            }
        }
    }
    
    private fun findCodeDuplicates(): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        // Простой алгоритм поиска дублирования - ищем файлы с похожими именами или размерами
        val codeFiles = session.projectStructure.files.filter { 
            it.language in setOf("Kotlin", "Java", "JavaScript", "TypeScript", "Python", "C++", "C#")
        }
        
        val suspiciousDuplicates = mutableListOf<String>()
        
        // Группируем файлы по размеру
        val sizeGroups = codeFiles.groupBy { it.size }
        sizeGroups.forEach { (size, files) ->
            if (files.size > 1 && size > 1000) { // Файлы больше 1KB
                suspiciousDuplicates.add("📄 Файлы одинакового размера ($size байт):\n${files.joinToString("\n") { "  - ${it.relativePath}" }}")
            }
        }
        
        // Ищем файлы с похожими именами
        val nameGroups = codeFiles.groupBy { 
            it.relativePath.substringAfterLast("/").substringBeforeLast(".")
        }
        nameGroups.forEach { (baseName, files) ->
            if (files.size > 1) {
                suspiciousDuplicates.add("📝 Файлы с похожими именами '$baseName':\n${files.joinToString("\n") { "  - ${it.relativePath}" }}")
            }
        }
        
        return if (suspiciousDuplicates.isEmpty()) {
            "✅ Явных дубликатов кода не найдено (поверхностный анализ)"
        } else {
            "🔍 **Потенциальные дубликаты кода:**\n\n" + suspiciousDuplicates.joinToString("\n\n")
        }
    }
    
    private fun checkArchitecture(): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        val structure = session.projectStructure
        
        return buildString {
            appendLine("🏗️ **Анализ архитектуры проекта:**")
            appendLine()
            
            // Анализ структуры папок
            val directories = structure.files.map { 
                it.relativePath.substringBeforeLast("/", "")
            }.filter { it.isNotEmpty() }.distinct().sorted()
            
            appendLine("📁 **Структура каталогов:**")
            directories.take(20).forEach { dir ->
                val filesInDir = structure.files.count { it.relativePath.startsWith("$dir/") }
                appendLine("- $dir/ ($filesInDir файлов)")
            }
            
            appendLine()
            appendLine("📊 **Статистика по языкам:**")
            structure.languages.entries.sortedByDescending { it.value }.forEach { (lang, count) ->
                val percentage = (count * 100.0 / structure.totalFiles).let { "%.1f".format(it) }
                appendLine("- $lang: $count файлов ($percentage%)")
            }
            
            appendLine()
            appendLine("💡 **Рекомендации будут даны после детального анализа ключевых файлов**")
        }
    }
    
    private fun generateFinalReport(): String {
        val session = currentSession ?: return "❌ Сессия не активна"
        
        return buildString {
            appendLine("📋 **ФИНАЛЬНЫЙ ОТЧЁТ РЕВЬЮ ПРОЕКТА**")
            appendLine("=".repeat(50))
            appendLine()
            
            appendLine("**Проект:** ${session.projectStructure.rootPath}")
            appendLine("**Проанализировано файлов:** ${session.reviewedFiles.size} из ${session.projectStructure.totalFiles}")
            appendLine("**Всего строк кода:** ${session.projectStructure.totalLines}")
            appendLine()
            
            appendLine("**Основные языки:**")
            session.projectStructure.languages.entries.sortedByDescending { it.value }.take(5).forEach { (lang, count) ->
                appendLine("- $lang: $count файлов")
            }
            appendLine()
            
            if (session.findings.isNotEmpty()) {
                appendLine("**Найденные проблемы:**")
                session.findings.groupBy { it.severity }.forEach { (severity, findings) ->
                    appendLine("${severity.name} (${findings.size}):")
                    findings.forEach { finding ->
                        appendLine("- ${finding.type}: ${finding.description}")
                    }
                }
            } else {
                appendLine("**Рекомендации:**")
                appendLine("- Проведите более детальный анализ ключевых файлов")
                appendLine("- Используйте статические анализаторы кода для вашего языка")
                appendLine("- Регулярно обновляйте зависимости")
                appendLine("- Добавьте автоматические тесты если их нет")
            }
        }
    }
    
    private fun isIgnoredPath(path: String): Boolean {
        val ignoredPatterns = listOf(
            ".git", ".svn", ".hg",
            "node_modules", ".gradle", "build",
            "target", "dist", "out",
            ".idea", ".vscode", ".vs",
            "venv", ".venv", "__pycache__", "Pods",
            "*.log", "*.tmp", "*.cache"
        )
        
        return ignoredPatterns.any { pattern ->
            path.contains("/$pattern/") || path.endsWith("/$pattern") || 
            (pattern.startsWith("*.") && path.endsWith(pattern.drop(1)))
        }
    }
    
    private fun detectLanguage(extension: String, fileName: String): String {
        return when (extension.lowercase()) {
            "kt" -> "Kotlin"
            "java" -> "Java"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "py" -> "Python"
            "cpp", "cc", "cxx" -> "C++"
            "c" -> "C"
            "h", "hpp" -> "C/C++ Header"
            "cs" -> "C#"
            "go" -> "Go"
            "rs" -> "Rust"
            "swift" -> "Swift"
            "php" -> "PHP"
            "rb" -> "Ruby"
            "scala" -> "Scala"
            "clj" -> "Clojure"
            "hs" -> "Haskell"
            "ml" -> "OCaml"
            "r" -> "R"
            "sql" -> "SQL"
            "html" -> "HTML"
            "css" -> "CSS"
            "scss" -> "SCSS"
            "less" -> "Less"
            "xml" -> "XML"
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "gradle" -> "Gradle"
            else -> when (fileName.lowercase()) {
                "dockerfile" -> "Docker"
                "makefile" -> "Makefile"
                "package.json" -> "NPM Package"
                "pom.xml" -> "Maven POM"
                "requirements.txt" -> "Python Requirements"
                else -> "Unknown"
            }
        }
    }
    
    private fun countLines(file: File): Int {
        return try {
            file.readLines().size
        } catch (e: Exception) {
            0
        }
    }
}
