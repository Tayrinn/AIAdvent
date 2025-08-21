package com.tayrinn.aiadvent.service

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Сервис для работы с файлами
 */
class FileService {
    
    /**
     * Читает содержимое файла
     */
    fun readFile(filePath: String): String {
        return try {
            Files.readString(Paths.get(filePath))
        } catch (e: Exception) {
            "Ошибка чтения файла: ${e.message}"
        }
    }
    
    /**
     * Записывает текст в файл
     */
    fun writeFile(filePath: String, content: String): Boolean {
        return try {
            Files.write(Paths.get(filePath), content.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            true
        } catch (e: Exception) {
            println("Ошибка записи файла: ${e.message}")
            false
        }
    }
    
    /**
     * Генерирует имя файла для тестов
     */
    fun generateTestFileName(originalFileName: String): String {
        val nameWithoutExt = originalFileName.substringBeforeLast(".")
        val extension = originalFileName.substringAfterLast(".", "kt")
        return "${nameWithoutExt}Test.kt"
    }
    
    /**
     * Создает директорию для тестов, если её нет
     */
    fun ensureTestDirectoryExists(baseDir: String): String {
        val testDir = File(baseDir, "test")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        return testDir.absolutePath
    }
    
    /**
     * Получает расширение файла
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }
    
    /**
     * Проверяет, является ли файл исходным кодом
     */
    fun isSourceCodeFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName).lowercase()
        return extension in listOf("kt", "java", "py", "js", "ts", "cpp", "c", "cs", "go", "rs")
    }
}
