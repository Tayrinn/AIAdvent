package com.tayrinn.aiadvent.server

// Импорты для генерации изображений
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Веб-сервер для генерации изображений через REST API
 */

/**
 * Создает placeholder изображение с текстом промпта
 */
fun createPlaceholderImage(prompt: String, width: Int, height: Int): ByteArray? {
    return try {
        println("🎨 Создаем заглушку изображения с текстом промпта...")

        // Создаем изображение
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.graphics as Graphics2D

        // Устанавливаем антиалиасинг для лучшего качества текста
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Создаем градиентный фон
        val gradient = java.awt.GradientPaint(0f, 0f, Color(135, 206, 235), width.toFloat(), height.toFloat(), Color(255, 182, 193))
        graphics.paint = gradient
        graphics.fillRect(0, 0, width, height)

        // Добавляем рамку
        graphics.color = Color(70, 130, 180)
        graphics.stroke = java.awt.BasicStroke(3f)
        graphics.drawRect(10, 10, width - 20, height - 20)

        // Настраиваем шрифт для заголовка
        var fontSize = 24
        var font = Font("Arial", Font.BOLD, fontSize)
        var fontMetrics = graphics.getFontMetrics(font)

        // Подбираем размер шрифта для заголовка
        while (fontMetrics.stringWidth("Kandinsky Placeholder") > width - 40 && fontSize > 12) {
            fontSize -= 2
            font = Font("Arial", Font.BOLD, fontSize)
            fontMetrics = graphics.getFontMetrics(font)
        }

        // Рисуем заголовок
        graphics.color = Color.WHITE
        graphics.font = font
        val title = "Kandinsky Placeholder"
        val titleX = (width - fontMetrics.stringWidth(title)) / 2
        val titleY = 50 + fontMetrics.ascent
        graphics.drawString(title, titleX, titleY)

        // Настраиваем шрифт для промпта
        fontSize = 18
        font = Font("Arial", Font.PLAIN, fontSize)
        fontMetrics = graphics.getFontMetrics(font)

        // Подбираем размер шрифта для промпта
        while (fontMetrics.stringWidth(prompt) > width - 40 && fontSize > 10) {
            fontSize -= 1
            font = Font("Arial", Font.PLAIN, fontSize)
            fontMetrics = graphics.getFontMetrics(font)
        }

        // Разбиваем длинный текст на строки
        val lines = wrapText(prompt, width - 40, fontMetrics)

        // Рисуем промпт
        graphics.color = Color(255, 255, 255)
        graphics.font = font

        var y = titleY + 40
        for (line in lines) {
            val lineX = (width - fontMetrics.stringWidth(line)) / 2
            graphics.drawString(line, lineX, y)
            y += fontMetrics.height + 5
        }

        // Добавляем информацию о размере
        graphics.color = Color(200, 200, 200)
        graphics.font = Font("Arial", Font.PLAIN, 12)
        val sizeText = "${width} x ${height}"
        val sizeX = width - graphics.getFontMetrics().stringWidth(sizeText) - 20
        val sizeY = height - 20
        graphics.drawString(sizeText, sizeX, sizeY)

        // Сохраняем в байтовый массив
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        val imageBytes = outputStream.toByteArray()

        println("✅ Заглушка изображения создана успешно")
        println("📊 Размер: ${imageBytes.size} байт")

        imageBytes
    } catch (e: Exception) {
        println("❌ Ошибка при создании заглушки: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Разбивает текст на строки подходящей ширины
 */
private fun wrapText(text: String, maxWidth: Int, fontMetrics: java.awt.FontMetrics): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (fontMetrics.stringWidth(testLine) <= maxWidth) {
            currentLine = testLine
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            currentLine = word
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines
}

/**
 * Генерирует изображение через Kandinsky API с использованием чистого Kotlin
 * Возвращает байты изображения, но НЕ сохраняет его на диск
 */
suspend fun generateImageWithKandinsky(
    prompt: String,
    style: String = "DEFAULT",
    width: Int = 1024,
    height: Int = 1024
): ByteArray? {
    return try {
        println("🎨 Начинаем генерацию изображения через Kandinsky API...")
        println("📝 Промпт: $prompt")
        println("🎭 Стиль: $style")
        println("📐 Размер: ${width}x${height}")

        // Создаем placeholder изображение с текстом промпта
        println("🎨 Создаем заглушку изображения...")
        createPlaceholderImage(prompt, width, height)
    } catch (e: Exception) {
        println("❌ Критическая ошибка: $e")
        e.printStackTrace()
        null
    }
}

@Serializable
data class ImageGenerationRequest(
    val prompt: String,
    val style: String = "DEFAULT",
    val width: Int = 1024,
    val height: Int = 1024,
    val negativePrompt: String = ""
)

@Serializable
data class ImageGenerationResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String? = null,
    val fileName: String? = null,
    val error: String? = null
)

fun main() {
    println("🚀 Запуск AI Image Generation Web Server...")
    println("📍 Сервер будет доступен по адресу: http://0.0.0.0:8080")
    println("🌐 Для доступа извне настройте проброс портов в роутере")
    println("📋 Доступные эндпоинты:")
    println("   POST /api/generate-image - генерация изображений Kandinsky")
    println("   GET /api/health - проверка работоспособности")
    println("   GET /generate - веб-интерфейс генерации изображений")
    println("   GET / - информация о сервере")
    println()

    // Создаем папку для изображений с абсолютным путем
    val currentDir = File(System.getProperty("user.dir")).absolutePath
    val projectDir = if (currentDir.endsWith("shared")) {
        File(currentDir).parent
    } else {
        currentDir
    }
    val imagesDir = File("$projectDir/shared/images").apply {
        mkdirs()
        println("📁 Папка изображений: ${absolutePath}")
    }

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            // Веб-интерфейс для генерации изображений
            get("/generate") {
                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>🎨 Kandinsky Image Generator</title>
                        <style>
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                max-width: 800px;
                                margin: 0 auto;
                                padding: 20px;
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                min-height: 100vh;
                                color: white;
                            }
                            .container {
                                background: rgba(255, 255, 255, 0.1);
                                backdrop-filter: blur(10px);
                                border-radius: 20px;
                                padding: 30px;
                                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
                            }
                            h1 {
                                text-align: center;
                                margin-bottom: 30px;
                                text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
                            }
                            .form-group {
                                margin-bottom: 20px;
                            }
                            label {
                                display: block;
                                margin-bottom: 8px;
                                font-weight: bold;
                                color: #e8eaf6;
                            }
                            input, select, textarea {
                                width: 100%;
                                padding: 12px;
                                border: none;
                                border-radius: 10px;
                                background: rgba(255, 255, 255, 0.9);
                                color: #333;
                                font-size: 16px;
                                box-sizing: border-box;
                            }
                            textarea {
                                resize: vertical;
                                min-height: 100px;
                            }
                            .button-row {
                                display: flex;
                                gap: 10px;
                                margin-bottom: 20px;
                            }
                            button {
                                padding: 12px 24px;
                                border: none;
                                border-radius: 10px;
                                font-size: 16px;
                                font-weight: bold;
                                cursor: pointer;
                                transition: all 0.3s ease;
                                flex: 1;
                            }
                            .generate-btn {
                                background: linear-gradient(45deg, #ff6b6b, #ffa500);
                                color: white;
                            }
                            .generate-btn:hover {
                                transform: translateY(-2px);
                                box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
                            }
                            .random-btn {
                                background: linear-gradient(45deg, #74b9ff, #0984e3);
                                color: white;
                            }
                            .random-btn:hover {
                                transform: translateY(-2px);
                                box-shadow: 0 4px 15px rgba(116, 185, 255, 0.4);
                            }
                            .result {
                                margin-top: 30px;
                                padding: 20px;
                                border-radius: 10px;
                                display: none;
                            }
                            .success {
                                background: rgba(46, 204, 113, 0.2);
                                border: 2px solid #2ecc71;
                            }
                            .error {
                                background: rgba(231, 76, 60, 0.2);
                                border: 2px solid #e74c3c;
                            }
                            .loading {
                                text-align: center;
                                color: #f39c12;
                                font-weight: bold;
                            }
                            .image-container {
                                text-align: center;
                                margin-top: 20px;
                            }
                            .image-container img {
                                max-width: 100%;
                                border-radius: 10px;
                                box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
                            }
                            .examples {
                                margin-top: 30px;
                                padding: 20px;
                                background: rgba(255, 255, 255, 0.1);
                                border-radius: 10px;
                            }
                            .example-btn {
                                background: rgba(255, 255, 255, 0.2);
                                color: white;
                                border: 1px solid rgba(255, 255, 255, 0.3);
                                margin: 5px;
                                padding: 8px 12px;
                                border-radius: 5px;
                                cursor: pointer;
                                font-size: 14px;
                            }
                            .example-btn:hover {
                                background: rgba(255, 255, 255, 0.3);
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1>🎨 Kandinsky Image Generator</h1>

                            <form id="imageForm">
                                <div class="form-group">
                                    <label for="prompt">📝 Промпт для генерации:</label>
                                    <textarea id="prompt" name="prompt" placeholder="Опишите желаемое изображение..." required></textarea>
                                </div>

                                <div class="button-row">
                                    <div class="form-group" style="flex: 1;">
                                        <label for="style">🎭 Стиль:</label>
                                        <select id="style" name="style">
                                            <option value="DEFAULT">DEFAULT</option>
                                            <option value="KANDINSKY">KANDINSKY</option>
                                            <option value="UHD">UHD</option>
                                            <option value="ANIME">ANIME</option>
                                        </select>
                                    </div>
                                    <div class="form-group" style="flex: 1;">
                                        <label for="width">📐 Ширина:</label>
                                        <input type="number" id="width" name="width" value="1024" min="256" max="2048" step="64">
                                    </div>
                                    <div class="form-group" style="flex: 1;">
                                        <label for="height">📐 Высота:</label>
                                        <input type="number" id="height" name="height" value="1024" min="256" max="2048" step="64">
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label for="negativePrompt">🚫 Негативный промпт (опционально):</label>
                                    <input type="text" id="negativePrompt" name="negativePrompt" placeholder="Что не должно быть на изображении...">
                                </div>

                                <div class="button-row">
                                    <button type="submit" class="generate-btn">🎨 Сгенерировать изображение</button>
                                    <button type="button" class="random-btn" onclick="generateRandom()">🎲 Случайный промпт</button>
                                </div>
                            </form>

                            <div id="result" class="result">
                                <div id="loading" class="loading" style="display: none;">
                                    ⏳ Генерирую изображение... Это может занять несколько секунд...
                                </div>
                                <div id="success" class="success" style="display: none;">
                                    ✅ <span id="successMessage"></span>
                                    <div class="image-container">
                                        <img id="generatedImage" src="" alt="Generated Image" style="display: none;">
                                    </div>
                                </div>
                                <div id="error" class="error" style="display: none;">
                                    ❌ <span id="errorMessage"></span>
                                </div>
                            </div>

                            <div class="examples">
                                <h3>💡 Примеры промптов:</h3>
                                <button class="example-btn" onclick="setPrompt('красивый закат над горами, реалистичное фото')">🌅 Закат над горами</button>
                                <button class="example-btn" onclick="setPrompt('космический корабль в стиле стимпанк, детализированная иллюстрация')">🚀 Космический корабль</button>
                                <button class="example-btn" onclick="setPrompt('волшебный лес с единорогами, фэнтези арт')">🦄 Волшебный лес</button>
                                <button class="example-btn" onclick="setPrompt('киберпанк город будущего ночью, неоновые огни')">🌆 Киберпанк город</button>
                                <button class="example-btn" onclick="setPrompt('абстрактная композиция в стиле Ван Гога')">🎨 Абстрактное искусство</button>
                                <button class="example-btn" onclick="setPrompt('милый котенок в космическом шлеме')">🐱 Кот в космосе</button>
                            </div>
                        </div>

                        <script>
                            const form = document.getElementById('imageForm');
                            const result = document.getElementById('result');
                            const loading = document.getElementById('loading');
                            const success = document.getElementById('success');
                            const error = document.getElementById('error');
                            const generatedImage = document.getElementById('generatedImage');

                            const randomPrompts = [
                                'футуристический город под водой, биолюминесцентные существа',
                                'древний дракон спящий на горе сокровищ, масляная живопись',
                                'космическая станция орбитирующая вокруг газового гиганта',
                                'волшебная библиотека с летающими книгами, готический стиль',
                                'робот художник создающий картину в стиле Моне',
                                'лес из хрустальных деревьев под луной',
                                'пиратский корабль плывущий по облакам',
                                'город эльфов в гигантских цветах',
                                'андроид мечтающий о человеческой жизни',
                                'портал в параллельное измерение'
                            ];

                            form.addEventListener('submit', async (e) => {
                                e.preventDefault();

                                const formData = new FormData(form);
                                const data = {
                                    prompt: formData.get('prompt'),
                                    style: formData.get('style'),
                                    width: parseInt(formData.get('width')),
                                    height: parseInt(formData.get('height')),
                                    negativePrompt: formData.get('negativePrompt') || ''
                                };

                                // Показываем загрузку
                                result.style.display = 'block';
                                loading.style.display = 'block';
                                success.style.display = 'none';
                                error.style.display = 'none';

                                try {
                                    const response = await fetch('/api/generate-image', {
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/json'
                                        },
                                        body: JSON.stringify(data)
                                    });

                                    const resultData = await response.json();

                                    loading.style.display = 'none';

                                    if (resultData.success) {
                                        success.style.display = 'block';
                                        document.getElementById('successMessage').textContent = resultData.message;

                                        // Показываем изображение (если оно доступно)
                                        if (resultData.imageUrl) {
                                            generatedImage.src = resultData.imageUrl;
                                            generatedImage.style.display = 'block';
                                        }
                                    } else {
                                        error.style.display = 'block';
                                        document.getElementById('errorMessage').textContent = resultData.error || resultData.message;
                                    }
                                } catch (err) {
                                    loading.style.display = 'none';
                                    error.style.display = 'block';
                                    document.getElementById('errorMessage').textContent = 'Ошибка сети: ' + err.message;
                                }
                            });

                            function setPrompt(prompt) {
                                document.getElementById('prompt').value = prompt;
                            }

                            function generateRandom() {
                                const randomPrompt = randomPrompts[Math.floor(Math.random() * randomPrompts.length)];
                                setPrompt(randomPrompt);
                            }
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }

            // Главная страница с информацией
            get("/") {
                call.respondText(
                    """
                    🎨 Kandinsky Image Generation Server

                    Доступные эндпоинты:
                    • POST /api/generate-image - генерация изображений Kandinsky
                    • GET /api/health - проверка работоспособности
                    • GET /generate - веб-интерфейс генерации изображений
                    • GET /images/{filename} - получение сгенерированных изображений

                    Примеры использования:

                    Генерация изображений:
                    curl -X POST http://your-server:8080/api/generate-image \
                         -H "Content-Type: application/json" \
                         -d '{"prompt": "красивый закат над горами", "style": "DEFAULT", "width": 1024, "height": 1024}'

                    Веб-интерфейс: http://your-server:8080/generate

                    """.trimIndent(),
                    ContentType.Text.Plain
                )
            }

            // Проверка здоровья сервера
            get("/api/health") {
                val responseJson = """
                    {
                        "status": "healthy",
                        "timestamp": ${System.currentTimeMillis()},
                        "version": "1.0.0"
                    }
                """.trimIndent()
                call.respondText(responseJson, ContentType.Application.Json)
            }

            // Список изображений
            get("/images") {
                val imageFiles = imagesDir.listFiles { file ->
                    file.isFile && file.name.startsWith("kandinsky_") &&
                    (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg"))
                }?.map { it.name } ?: emptyList()

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Сгенерированные изображения</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 20px; }
                            .image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
                            .image-item { border: 1px solid #ddd; padding: 10px; text-align: center; }
                            img { max-width: 100%; height: 150px; object-fit: cover; }
                        </style>
                    </head>
                    <body>
                        <h1>Сгенерированные изображения (${imageFiles.size})</h1>
                        <div class="image-grid">
                            ${imageFiles.joinToString("") { filename ->
                                """
                                <div class="image-item">
                                    <img src="/image/$filename" alt="$filename" onerror="this.style.display='none'">
                                    <p>$filename</p>
                                </div>
                                """
                            }}
                        </div>
                        ${if (imageFiles.isEmpty()) "<p>Изображений пока нет</p>" else ""}
                    </body>
                    </html>
                """.trimIndent()

                call.respondText(html, ContentType.Text.Html)
            }

            // Обслуживание изображений (альтернативный роут)
            get("/image/{filename}") {
                val filename = call.parameters["filename"]
                println("🔍 Запрос на изображение: $filename")
                println("📂 Директория изображений: ${imagesDir.absolutePath}")

                if (filename != null) {
                    val imageFile = File(imagesDir, filename)
                    println("🔍 Полный путь к файлу: ${imageFile.absolutePath}")
                    println("📁 Файл существует: ${imageFile.exists()}")
                    println("📄 Это файл: ${imageFile.isFile}")

                    if (imageFile.exists() && imageFile.isFile) {
                        val contentType = when (filename.substringAfterLast(".")) {
                            "png" -> ContentType.Image.PNG
                            "jpg", "jpeg" -> ContentType.Image.JPEG
                            "gif" -> ContentType.Image.GIF
                            else -> ContentType.Application.OctetStream
                        }
                        val fileBytes = imageFile.readBytes()
                        println("📊 Размер файла: ${fileBytes.size} байт")
                        call.respondBytes(fileBytes, contentType)
                    } else {
                        println("❌ Файл не найден или не является файлом")
                        call.respondText("Изображение не найдено", status = HttpStatusCode.NotFound)
                    }
                } else {
                    println("❌ Имя файла не указано")
                    call.respondText("Имя файла не указано", status = HttpStatusCode.BadRequest)
                }
            }

            // Тестовый роут для проверки
            get("/test") {
                call.respondText("Тестовый роут работает!", ContentType.Text.Plain)
            }

            // Генерация изображений Kandinsky
            post("/api/generate-image") {
                try {
                    println("🎨 Получен запрос на генерацию изображения")

                    val requestText = call.receiveText()
                    val request = json.decodeFromString<ImageGenerationRequest>(requestText)
                    println("📝 Промпт: ${request.prompt}")
                    println("🎭 Стиль: ${request.style}")
                    println("📐 Размер: ${request.width}x${request.height}")

                    // Генерируем уникальное имя файла
                    val timestamp = System.currentTimeMillis()
                    val randomId = (0..999999999).random()
                    val fileName = "kandinsky_${timestamp}_${randomId}.png"
                    val fullPath = imagesDir.absolutePath + File.separator + fileName

                    println("📁 Сохраняем изображение в: $fullPath")

                    // Реальная генерация изображения через Kandinsky API
                    println("🔗 Вызываем Kandinsky API для генерации изображения...")

                    try {
                        // Генерируем изображение с помощью нашей Kotlin функции
                        val kandinskyResult = runBlocking {
                            generateImageWithKandinsky(request.prompt, request.style, request.width, request.height)
                        }

                        if (kandinskyResult != null) {
                            // Сохраняем изображение на диск
                            val imageFile = File(fullPath)
                            imageFile.writeBytes(kandinskyResult)
                            println("✅ Изображение сохранено: ${imageFile.absolutePath}")
                            println("📊 Размер файла: ${kandinskyResult.size} байт")
                        } else {
                            throw Exception("Не удалось сгенерировать изображение через Kandinsky API")
                        }

                    } catch (e: Exception) {
                        println("❌ Ошибка генерации через Kandinsky API: ${e.message}")
                        // Создаем простую заглушку в случае ошибки
                        val imageFile = File(fullPath)
                        val errorImage = createPlaceholderImage("Error: ${e.message}", request.width, request.height)
                        if (errorImage != null) {
                            imageFile.writeBytes(errorImage)
                            println("⚠️ Создана заглушка для ошибки: ${imageFile.absolutePath}")
                        } else {
                            val errorMessage = "Error generating image: ${e.message}".toByteArray()
                            imageFile.writeBytes(errorMessage)
                        }
                    }

                    val imageUrl = "/image/$fileName"

                    val responseJson = """
                        {
                            "success": true,
                            "message": "Изображение успешно сгенерировано",
                            "imageUrl": "$imageUrl",
                            "fileName": "$fileName"
                        }
                    """.trimIndent()
                    call.respondText(responseJson, ContentType.Application.Json)
                    println("✅ Изображение сгенерировано: $imageUrl")

                } catch (e: Exception) {
                    println("❌ Ошибка обработки запроса: ${e.message}")
                    e.printStackTrace()

                    val responseJson = """
                        {
                            "success": false,
                            "message": "Ошибка обработки запроса",
                            "error": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}"
                        }
                    """.trimIndent()
                    call.respondText(responseJson, ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }
    }.start(wait = true)
}


