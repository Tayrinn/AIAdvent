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
import auth.WebGoogleAuthService
import auth.DesktopUser
import ui.DesktopAuthScreen
import ui.DesktopAuthLoadingScreen
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
@Preview
fun App() {
    val authService = remember { WebGoogleAuthService() }
    var currentUser by remember { mutableStateOf<DesktopUser?>(null) }
    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    
    // Проверяем состояние авторизации при запуске
    LaunchedEffect(Unit) {
        val user = authService.getCurrentUser()
        if (user != null) {
            currentUser = user
            authState = AuthState.Authenticated
        } else {
            authState = AuthState.Unauthenticated
        }
    }
    
    when (authState) {
        AuthState.Loading -> {
            DesktopAuthLoadingScreen()
        }
        
        AuthState.Unauthenticated -> {
            DesktopAuthScreen(
                authService = authService,
                onAuthSuccess = { user ->
                    currentUser = user
                    authState = AuthState.Authenticated
                    println("✅ Пользователь авторизован: ${user.name} (${user.email})")
                },
                onAuthError = { error ->
                    println("❌ Ошибка авторизации: $error")
                    authState = AuthState.Error(error)
                }
            )
        }
        
        AuthState.Authenticated -> {
            currentUser?.let { user ->
                MainAppContent(
                    user = user,
                    onSignOut = {
                        authService.signOut()
                        currentUser = null
                        authState = AuthState.Unauthenticated
                    }
                )
            }
        }
        
        is AuthState.Error -> {
            DesktopAuthScreen(
                authService = authService,
                onAuthSuccess = { user ->
                    currentUser = user
                    authState = AuthState.Authenticated
                },
                onAuthError = { error ->
                    println("❌ Ошибка авторизации: $error")
                }
            )
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@Composable
fun MainAppContent(
    user: DesktopUser,
    onSignOut: () -> Unit
) {
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
    
    // Создаем сервисы для работы с предпочтениями пользователя
    val preferencesStorage = remember { com.tayrinn.aiadvent.data.local.PreferencesStorage() }
    val preferencesExtractionService = remember { com.tayrinn.aiadvent.service.PreferencesExtractionService() }
    
    // Состояние для предпочтений пользователя
    var userPreferences by remember { mutableStateOf<com.tayrinn.aiadvent.data.model.UserPreferences?>(null) }

    // Функция для создания системного промпта с предпочтениями
    val createSystemPrompt: (String) -> String = { basePrompt ->
        val preferences = userPreferences
        if (preferences != null) {
            val preferencesText = buildString {
                append("ПРЕДПОЧТЕНИЯ ПОЛЬЗОВАТЕЛЯ:\n")
                preferences.name?.let { append("- Имя: $it\n") }
                append("- Язык общения: ${preferences.language}\n")
                append("- Стиль общения: ${preferences.communicationStyle}\n")
                append("- Предпочитаемая длина ответов: ${preferences.responseLength}\n")
                
                if (preferences.interests.isNotEmpty()) {
                    append("- Интересы: ${preferences.interests.joinToString(", ")}\n")
                }
                if (preferences.expertise.isNotEmpty()) {
                    append("- Области экспертизы: ${preferences.expertise.joinToString(", ")}\n")
                }
                if (preferences.preferredTopics.isNotEmpty()) {
                    append("- Предпочитаемые темы: ${preferences.preferredTopics.joinToString(", ")}\n")
                }
                if (preferences.avoidTopics.isNotEmpty()) {
                    append("- Избегать тем: ${preferences.avoidTopics.joinToString(", ")}\n")
                }
                append("\n")
            }
            "$preferencesText$basePrompt"
        } else {
            basePrompt
        }
    }
    
    // Функция для воспроизведения аудио
    val playAudio: (String) -> Unit = { text ->
        scope.launch {
            try {
                println("🔊 Генерируем аудио для: ${text.take(50)}...")
                val audioFilePath = speechToTextService.generateSpeech(text)
                // Воспроизводим аудио файл
                playAudioFile(audioFilePath)
            } catch (e: Exception) {
                println("❌ Ошибка воспроизведения: ${e.message}")
            }
        }
    }


    
    // Загружаем сообщения из хранилища и предпочтения пользователя
    LaunchedEffect(Unit) {
        try {
            // Загружаем предпочтения пользователя
            userPreferences = preferencesStorage.loadPreferences(user.id)
            if (userPreferences == null) {
                // Создаем начальные предпочтения на основе данных Google
                userPreferences = com.tayrinn.aiadvent.data.model.UserPreferences(
                    userId = user.id,
                    name = user.name,
                    language = "ru" // По умолчанию русский язык
                )
                preferencesStorage.savePreferences(userPreferences!!)
                println("✅ Созданы начальные предпочтения для пользователя: ${user.name}")
            } else {
                println("📖 Загружены предпочтения пользователя: ${userPreferences?.name}")
            }
            
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
            // Заголовок с информацией о пользователе
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Информация о пользователе
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватар пользователя (иконка)
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Пользователь",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "AIAdvent Desktop",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Привет, ${user.name}!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
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
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Кнопка выхода
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Выйти",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выйти")
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
                    ChatMessageItem(message = message, onPlayAudio = playAudio)
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
                            scope.launch { kotlinx.coroutines.delay(500) }

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
                                    // Отправляем сообщение к ChatGPT в трёхэтапном процессе
                                    scope.launch {
                                        isLoading.value = true
                                        try {
                                            // Получаем последние 3 сообщения для контекста (исключая текущее)
                                            val recentMessages = messages.takeLast(3).filter { it.content != text }

                                            // Этап 1: Размышления AI
                                            println("🤔 Этап 1: AI размышляет...")
                                            val baseThinkingPrompt = "Внимательно обдумай вопрос пользователя и напиши свои рассуждения. Проанализируй все аспекты вопроса. Напиши только рассуждения, без самого ответа или дополнительных вопросов пользователю."
                                            val thinkingPrompt = createSystemPrompt(baseThinkingPrompt)
                                            val (thinkingResponse, _) = chatRepository.sendMessage(
                                                "$thinkingPrompt\n\nВопрос пользователя: $text", 
                                                recentMessages, 
                                                modelName.value
                                            )

                                            val thinkingMessage = ChatMessage(
                                                content = "🤖 **Думает:** $thinkingResponse",
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                            messages.add(thinkingMessage)
                                            chatStorage.saveMessage(thinkingMessage)

                                            kotlinx.coroutines.delay(1000)

                                            // Этап 2: Проверка рассуждений
                                            println("🔍 Этап 2: AI проверяет рассуждения...")
                                            val baseConfirmationPrompt = "Прочти свои рассуждения и подтверди, что они верные. Найди возможные ошибки или упущения. Напиши только анализ и проверку рассуждений, без дополнительных вопросов или самого ответа."
                                            val confirmationPrompt = createSystemPrompt(baseConfirmationPrompt)
                                            val (confirmationResponse, _) = chatRepository.sendMessage(
                                                "$confirmationPrompt\n\nМои рассуждения: $thinkingResponse\n\nВопрос пользователя: $text", 
                                                emptyList(), // Не передаём контекст, чтобы сосредоточиться на проверке
                                                modelName.value
                                            )

                                            val confirmationMessage = ChatMessage(
                                                content = "🤖 **Ищу подтверждения:** $confirmationResponse",
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                            messages.add(confirmationMessage)
                                            chatStorage.saveMessage(confirmationMessage)

                                            kotlinx.coroutines.delay(1000)

                                            // Этап 3: Финальный ответ
                                            println("✅ Этап 3: AI даёт финальный ответ...")
                                            val baseFinalPrompt = "На основе своих рассуждений и их проверки дай полный и точный ответ пользователю."
                                            val finalPrompt = createSystemPrompt(baseFinalPrompt)
                                            val (finalResponse, _) = chatRepository.sendMessage(
                                                "$finalPrompt\n\nРассуждения: $thinkingResponse\n\nПроверка: $confirmationResponse\n\nВопрос пользователя: $text", 
                                                emptyList(), // Не передаём контекст, чтобы сосредоточиться на финальном ответе
                                                modelName.value
                                            )

                                            val finalMessage = ChatMessage(
                                                content = finalResponse,
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                            messages.add(finalMessage)
                                            chatStorage.saveMessage(finalMessage)

                                            // Извлекаем предпочтения из диалога в фоновом режиме
                                            scope.launch {
                                                try {
                                                    println("🧠 Извлекаем предпочтения пользователя из диалога...")
                                                    val extractedPreferences = preferencesExtractionService.extractPreferences(
                                                        messages = messages.toList(), // Создаем копию списка
                                                        currentPreferences = userPreferences,
                                                        userId = user.id
                                                    )
                                                    
                                                    if (extractedPreferences != null) {
                                                        userPreferences = extractedPreferences
                                                        preferencesStorage.savePreferences(extractedPreferences)
                                                        println("✅ Предпочтения обновлены и сохранены")
                                                    }
                                                } catch (e: Exception) {
                                                    println("❌ Ошибка извлечения предпочтений: ${e.message}")
                                                }
                                            }

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
fun ChatMessageItem(message: ChatMessage, onPlayAudio: (String) -> Unit = {}) {
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp
            )

            // Кнопка воспроизведения в правом верхнем углу
            IconButton(
                onClick = { onPlayAudio(message.content) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Воспроизвести сообщение",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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

/**
 * Воспроизводит аудио данные
 */
fun playAudioData(audioData: ByteArray) {
    try {
        println("🔊 Воспроизводим аудио (${audioData.size} байт)...")

        // Создаем временный файл для воспроизведения
        val tempFile = kotlin.io.path.createTempFile("tts_audio", ".mp3").toFile()
        tempFile.writeBytes(audioData)

        // Используем Java Sound API для воспроизведения
        val audioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(tempFile)
        val clip = javax.sound.sampled.AudioSystem.getClip()

        // Получаем информацию о формате аудио
        val format = audioInputStream.format
        println("📊 Формат аудио: ${format.sampleRate}Hz, ${format.channels} каналов, ${format.sampleSizeInBits} бит")

        clip.open(audioInputStream)
        clip.start()

        // Ждем окончания воспроизведения с небольшим запасом
        val durationMs = (clip.microsecondLength / 1000) + 500
        println("⏱️ Длительность воспроизведения: ${durationMs}мс")
        Thread.sleep(durationMs)

        clip.close()
        audioInputStream.close()
        tempFile.delete()

        println("✅ Воспроизведение завершено")
    } catch (e: Exception) {
        println("❌ Ошибка воспроизведения аудио: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Воспроизводит аудио файл по пути
 */
fun playAudioFile(audioFilePath: String) {
    try {
        println("🔊 Воспроизводим файл: $audioFilePath")

        val audioFile = java.io.File(audioFilePath)

        if (!audioFile.exists()) {
            println("❌ Файл не найден: $audioFilePath")
            return
        }

        // Определяем команду для воспроизведения в зависимости от ОС
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("mac") || os.contains("darwin") -> {
                // Используем afplay для macOS
                arrayOf("afplay", audioFilePath)
            }
            os.contains("linux") -> {
                // Используем aplay или mpg123 для Linux
                arrayOf("mpg123", audioFilePath)
            }
            os.contains("windows") -> {
                // Используем встроенный проигрыватель Windows
                arrayOf("cmd", "/c", "start", "/min", audioFilePath)
            }
            else -> {
                println("❌ Неподдерживаемая ОС: $os")
                return
            }
        }

        // Проверяем доступность команды
        val commandCheck = ProcessBuilder("which", command[0])
            .redirectErrorStream(true)
            .start()

        val checkExitCode = commandCheck.waitFor()
        if (checkExitCode != 0) {
            println("❌ Команда ${command[0]} не найдена в системе")
            return
        }

        println("🎵 Используем команду: ${command.joinToString(" ")}")

        // Запускаем процесс воспроизведения
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        // Читаем вывод процесса (для отладки)
        val reader = process.inputStream.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println("Audio: $line")
        }

        // Ждем завершения воспроизведения
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            println("✅ Воспроизведение завершено успешно")
        } else {
            println("⚠️ Процесс воспроизведения завершился с кодом: $exitCode")
        }

        // Удаляем файл после воспроизведения
        audioFile.delete()
        println("🗑️ Временный файл удален")

    } catch (e: Exception) {
        println("❌ Ошибка воспроизведения файла: ${e.message}")
        e.printStackTrace()
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
