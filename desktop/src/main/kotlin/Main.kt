import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.api.createOpenAIApiImpl
import com.tayrinn.aiadvent.data.repository.OpenAIChatRepository
import com.tayrinn.aiadvent.data.local.ChatStorage
import com.tayrinn.aiadvent.service.*
import com.tayrinn.aiadvent.util.TestRunner
import com.tayrinn.aiadvent.service.SpeechToTextService
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
@Preview
fun App() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val inputText = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val isRecording = remember { mutableStateOf(false) }
    val temperature = remember { mutableStateOf(0.7f) }
    val modelName = remember { mutableStateOf("deepseek-ai/DeepSeek-V3-0324") }
    val scope = rememberCoroutineScope()
    val testRunner = remember { TestRunner() }

    // Создаем хранилище для сообщений
    val chatStorage = remember { ChatStorage() }

    // Создаем OpenAI API и Repository
    val openAIApi = remember { createOpenAIApiImpl() }
    val chatRepository = remember { OpenAIChatRepository(openAIApi) }
    
    // Создаем сервисы для работы с тестами
    val fileService = remember { FileService() }
    val bugFixService = remember { BugFixService(chatRepository) }
    val testGenerationService = remember { TestGenerationService(openAIApi) }
    val testExecutionService = remember { TestExecutionService() }
    val testWorkflowService = remember { TestWorkflowService(fileService, bugFixService, testGenerationService, testExecutionService) }

    // Создаем сервис распознавания речи
    val speechToTextService = remember { SpeechToTextService() }


    
    // Загружаем сообщения из хранилища и добавляем приветственное сообщение
    LaunchedEffect(Unit) {
        try {
            val savedMessages = chatStorage.loadMessages()
            messages.addAll(savedMessages)

            // Добавляем приветственное сообщение только если нет сохраненных сообщений
            if (savedMessages.isEmpty()) {
                messages.add(
                    ChatMessage(
                        content = "🚀 **AIAdvent Desktop with Hugging Face:** Welcome! Now powered by Hugging Face API!",
                        isUser = false,
                        isAgent1 = true
                    )
                )
            }
        } catch (e: Exception) {
            println("❌ Ошибка загрузки сообщений: ${e.message}")
            // В случае ошибки показываем приветственное сообщение
            messages.add(
                ChatMessage(
                    content = "🚀 **AIAdvent Desktop with Hugging Face:** Welcome! Now powered by Hugging Face API!",
                    isUser = false,
                    isAgent1 = true
                )
            )
        }
    }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок и кнопки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AIAdvent Desktop + Hugging Face",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    Button(
                        onClick = {
                            scope.launch {
                                val selectedFile = selectFile()
                                if (selectedFile != null) {
                                    messages.add(
                                        ChatMessage(
                                            content = "📁 Выбран файл: $selectedFile",
                                            isUser = false,
                                            isAgent1 = true
                                        )
                                    )

                                    // Анализируем файл и генерируем тесты
                                    isLoading.value = true
                                    try {
                                        // Callback функция для добавления сообщений
                                        val onMessage: (String) -> Unit = { message: String ->
                                            messages.add(
                                                ChatMessage(
                                                    content = message,
                                                    isUser = false,
                                                    isAgent1 = true
                                                )
                                            )
                                        }

                                        val result = testWorkflowService.executeTestWorkflow(selectedFile, onMessage)

                                        // Добавляем итоговый отчет
                                        messages.add(
                                            ChatMessage(
                                                content = "📋 **ИТОГОВЫЙ ОТЧЕТ:**\n\n$result",
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                        )
                                    } catch (e: Exception) {
                                        messages.add(
                                            ChatMessage(
                                                content = "❌ Ошибка: ${e.message}",
                                                isUser = false,
                                                isError = true
                                            )
                                        )
                                    } finally {
                                        isLoading.value = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading.value
                    ) {
                        Text("📁 Открыть файл")
                    }


                }
            }
            
            // Ползунок температуры
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌡️ Температура модели:",
                    modifier = Modifier.width(150.dp),
                    fontSize = 14.sp
                )
                
                Slider(
                    value = temperature.value,
                    onValueChange = { temperature.value = it },
                    valueRange = 0.0f..2.0f,
                    steps = 19, // 0.1 шаг
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${String.format("%.1f", temperature.value)}",
                    modifier = Modifier.width(50.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Кнопка сброса температуры
                OutlinedButton(
                    onClick = { temperature.value = 0.7f },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Сброс")
                }
            }
            
            // Информация о температуре
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        temperature.value < 0.5f -> "❄️ Низкая креативность (точные ответы)"
                        temperature.value < 1.0f -> "🌤️ Средняя креативность (сбалансированные ответы)"
                        temperature.value < 1.5f -> "🔥 Высокая креативность (креативные ответы)"
                        else -> "💥 Максимальная креативность (очень креативные ответы)"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Поле ввода модели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 Модель AI:",
                    modifier = Modifier.width(120.dp),
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = modelName.value,
                    onValueChange = { modelName.value = it },
                    label = { Text("Название модели") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading.value,
                    placeholder = { Text("deepseek-ai/DeepSeek-V3-0324") }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Кнопка сброса модели
                OutlinedButton(
                    onClick = { modelName.value = "deepseek-ai/DeepSeek-V3-0324" },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Сброс")
                }
            }

            // Список сообщений
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = rememberLazyListState()
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Поле ввода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText.value,
                    onValueChange = { inputText.value = it },
                    label = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading.value && !isRecording.value
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка распознавания речи
                IconButton(
                    onClick = {
                        if (!isRecording.value && !isLoading.value) {
                            scope.launch {
                                try {
                                    isRecording.value = true
                                    println("🎤 Начинаем распознавание речи...")

                                    val transcribedText = speechToTextService.recordAndTranscribe(5)
                                    if (transcribedText.isNotBlank()) {
                                        inputText.value = transcribedText
                                        println("✅ Распознанный текст: \"$transcribedText\"")
                                    }
                                } catch (e: Exception) {
                                    println("❌ Ошибка распознавания речи: ${e.message}")
                                } finally {
                                    isRecording.value = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading.value,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording.value) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = if (isRecording.value) "Остановить запись" else "Распознать речь",
                        tint = if (isRecording.value) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = inputText.value.trim()
                        if (text.isNotEmpty() && !isRecording.value) {
                            // Добавляем сообщение пользователя
                            val userMessage = ChatMessage(
                                content = text,
                                isUser = true
                            )
                            messages.add(userMessage)

                            // Сохраняем сообщение пользователя в базу данных
                            scope.launch {
                                try {
                                    println("💾 Сохраняем пользовательское сообщение...")
                                    val savedMessage = chatStorage.saveMessage(userMessage)
                                    println("✅ Пользовательское сообщение сохранено с ID: ${savedMessage.id}")
                                } catch (e: Exception) {
                                    println("❌ Ошибка сохранения пользовательского сообщения: ${e.message}")
                                    e.printStackTrace()
                                }
                            }

                            // Небольшая задержка для сохранения
                            kotlinx.coroutines.delay(500)

                            // Очищаем поле ввода
                            inputText.value = ""
                            
                            // Проверяем специальные команды
                            when {
                                text.lowercase().contains("run tests") -> {
                                    scope.launch {
                                        isLoading.value = true
                                        try {
                                            val testReport = testRunner.runTests()
                                            messages.add(
                                                ChatMessage(
                                                    content = testReport.getSummary(),
                                                    isUser = false,
                                                    isTestReport = true
                                                )
                                            )
                                        } finally {
                                            isLoading.value = false
                                        }
                                    }
                                }
                                // Генерация изображений отключена
                                // text.lowercase().contains("generate") ||
                                // text.lowercase().contains("create") ||
                                // text.lowercase().contains("make") ||
                                // text.lowercase().contains("draw") ||
                                // text.lowercase().contains("picture") ||
                                // text.lowercase().contains("image") -> {
                                //     // Имитируем генерацию изображения
                                //     messages.add(
                                //         ChatMessage(
                                //             content = "🎨 **Image Generation:** Sorry, image generation is not available in desktop version yet.",
                                //             isUser = false,
                                //             isImageGeneration = true
                                //         )
                                //     )
                                // }
                                else -> {
                                    // Отправляем сообщение к ChatGPT
                                    scope.launch {
                                        isLoading.value = true
                                        try {
                                            // Получаем последние 3 сообщения для контекста (исключая текущее)
                                            val recentMessages = messages.takeLast(3).filter { it.content != text }
                                            val (agent1Response, _) = chatRepository.sendMessage(text, recentMessages, modelName.value)

                                            val aiMessage = ChatMessage(
                                                content = "🤖 **${modelName.value} (🌡️ ${String.format("%.1f", temperature.value)}):** $agent1Response",
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                            messages.add(aiMessage)

                                            // Сохраняем ответ AI в базу данных
                                            try {
                                                chatStorage.saveMessage(aiMessage)
                                            } catch (e: Exception) {
                                                println("❌ Ошибка сохранения ответа AI: ${e.message}")
                                            }

                                            // Небольшая задержка для сохранения
                                            kotlinx.coroutines.delay(500)
                                        } catch (e: Exception) {
                                            val errorMessage = ChatMessage(
                                                content = "❌ **Error:** ${e.message}",
                                                isUser = false,
                                                isError = true
                                            )
                                            messages.add(errorMessage)

                                            // Сохраняем сообщение об ошибке в базу данных
                                            try {
                                                chatStorage.saveMessage(errorMessage)
                                            } catch (e: Exception) {
                                                println("❌ Ошибка сохранения сообщения об ошибке: ${e.message}")
                                            }
                                        } finally {
                                            isLoading.value = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading.value && inputText.value.trim().isNotEmpty()
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val backgroundColor = when {
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        message.isAgent1 -> MaterialTheme.colorScheme.secondaryContainer
        message.isAgent2 -> MaterialTheme.colorScheme.tertiaryContainer
        message.isError -> Color(0xFFE57373)
        message.isImageGeneration -> Color(0xFF81C784)
        message.isTestReport -> Color(0xFF64B5F6)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = when {
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        message.isAgent1 -> MaterialTheme.colorScheme.onSecondaryContainer
        message.isAgent2 -> MaterialTheme.colorScheme.onTertiaryContainer
        message.isError -> Color.White
        message.isImageGeneration -> Color.White
        message.isTestReport -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = message.content,
            color = textColor,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp
        )
    }
}

/**
 * Функция для выбора файла через диалог
 */
fun selectFile(): String? {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Выберите файл с исходным кодом"
    fileChooser.fileFilter = FileNameExtensionFilter(
        "Исходный код", "kt", "java", "py", "js", "ts", "cpp", "c", "cs", "go", "rs"
    )
    
    val result = fileChooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile.absolutePath
    } else {
        null
    }
}

fun main() = application {
    val windowState = rememberWindowState()
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "AIAdvent Desktop"
    ) {
        App()
    }
}
