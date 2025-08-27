import java.io.File

// Тестирование парсинга функций
fun main() {
    val file = File("SimpleTestFile.kt")
    if (file.exists()) {
        val sourceCode = file.readText()
        println("✅ Файл найден! Размер: ${sourceCode.length} символов")
        println("\n📄 Содержимое файла:")
        sourceCode.lines().forEachIndexed { index, line ->
            println("   ${index + 1:2d}: $line")
        }

        println("\n🔍 Поиск функций:")
        var functionCount = 0
        sourceCode.lines().forEachIndexed { index, line ->
            if (line.contains("fun ")) {
                functionCount++
                val funIndex = line.indexOf("fun ")
                val parenStart = line.indexOf('(', funIndex)
                val parenEnd = line.indexOf(')', parenStart)
                println("   📍 Функция $functionCount на строке ${index + 1}:")
                println("      Строка: '$line'")
                println("      funIndex=$funIndex, parenStart=$parenStart, parenEnd=$parenEnd")

                if (parenEnd != -1) {
                    val signature = line.substring(funIndex, parenEnd + 1).trim()
                    println("      Сигнатура: '$signature'")

                    // Извлекаем имя функции
                    val afterFun = signature.substring(4)
                    val spaceIndex = afterFun.indexOf(' ')
                    val parenIndex = afterFun.indexOf('(')
                    val functionName = when {
                        spaceIndex != -1 && spaceIndex < parenIndex -> afterFun.substring(0, spaceIndex)
                        parenIndex != -1 -> afterFun.substring(0, parenIndex)
                        else -> "unknown"
                    }
                    println("      Имя функции: '$functionName'")
                } else {
                    println("      ❌ Ошибка: нет закрывающей скобки")
                }
                println()
            }
        }

        println("📊 Всего найдено функций: $functionCount")
    } else {
        println("❌ Файл SimpleTestFile.kt не найден!")
        println("   Текущая директория: ${System.getProperty("user.dir")}")
    }
}
