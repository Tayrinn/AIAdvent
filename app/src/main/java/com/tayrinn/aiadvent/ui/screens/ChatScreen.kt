package com.tayrinn.aiadvent.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tayrinn.aiadvent.R
import com.tayrinn.aiadvent.auth.GoogleUser
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.ui.viewmodel.ChatViewModel
import com.tayrinn.aiadvent.ui.viewmodel.ChatViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import com.tayrinn.aiadvent.data.repository.ChatRepository

@Composable
fun ChatScreen(
    user: GoogleUser,
    onSignOut: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(createRepository()))
) {
    Log.d("ChatScreen", "ChatScreen composable called")
    
                    val messages by viewModel.messages.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
                val apiLimits by viewModel.apiLimits.collectAsState()
    
    Log.d("ChatScreen", "collectAsState completed, messages.size = ${messages.size}")
    
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Загружаем сообщения при создании экрана
    LaunchedEffect(Unit) {
        Log.d("ChatScreen", "LaunchedEffect started")
        try {
            Log.d("ChatScreen", "About to call viewModel.loadMessages()")
            viewModel.loadMessages()
            Log.d("ChatScreen", "viewModel.loadMessages() called successfully")
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error in LaunchedEffect: ${e.message}")
        }
    }
    
    // Логируем изменения состояния
    LaunchedEffect(messages) {
        Log.d("ChatScreen", "Messages state changed: ${messages.size} messages")
    }
    
    LaunchedEffect(isLoading) {
        Log.d("ChatScreen", "Loading state changed: $isLoading")
    }
    
    LaunchedEffect(isGeneratingImage) {
        Log.d("ChatScreen", "Image generation state changed: $isGeneratingImage")
    }

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
                // Аватар пользователя
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Аватар пользователя",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Привет, ${user.displayName}!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
                                    Row {
                            // Отображение лимитов API
                            apiLimits?.let { limits ->
                                Card(
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (limits.isLimitReached) 
                                            MaterialTheme.colorScheme.errorContainer 
                                        else 
                                            MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (limits.isLimitReached) 
                                                MaterialTheme.colorScheme.onErrorContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${limits.remainingGenerations}/${limits.totalGenerations}",
                                            fontSize = 12.sp,
                                            color = if (limits.isLimitReached) 
                                                MaterialTheme.colorScheme.onErrorContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            // Кнопка очистки для тестирования
                            TextButton(
                                onClick = {
                                    Log.d("ChatScreen", "Clear messages button clicked")
                                    viewModel.clearMessages()
                                }
                            ) {
                                Text("Clear")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                            
                            // Кнопка выхода
                            IconButton(onClick = onSignOut) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Выйти",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
        }

        // Список сообщений
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Ограничиваем количество отображаемых сообщений для предотвращения ANR
            val displayMessages = messages.takeLast(50) // Показываем только последние 50 сообщений
            
            Log.d("ChatScreen", "Rendering ${displayMessages.size} messages out of ${messages.size} total")
            
            items(
                items = displayMessages.reversed(),
                key = { message -> message.id } // Добавляем ключи для оптимизации
            ) { message ->
                when {
                    message.isImageGeneration -> ImageMessageItem(message)
                    else -> MessageItem(message)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Индикатор загрузки
        if (isLoading || isGeneratingImage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGeneratingImage) "Generating image..." else "Thinking...",
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Кнопка отмены для генерации изображения
                if (isGeneratingImage) {
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = { 
                            // Отменяем текущую операцию
                            viewModel.cancelImageGeneration()
                        }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Поле ввода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (isGeneratingImage) "Generating image..." else "Type your message or request an image...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                enabled = !isLoading && !isGeneratingImage,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading && !isGeneratingImage
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }

        // Подсказки
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💡 Try these examples:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Ask a question: 'What is artificial intelligence?'",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Generate image: 'Сгенерируй изображение: кот в очках'",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Диалог настроек
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    val backgroundColor = when {
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        message.isAgent1 -> MaterialTheme.colorScheme.secondaryContainer
        message.isAgent2 -> MaterialTheme.colorScheme.tertiaryContainer
        message.isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        message.isAgent1 -> MaterialTheme.colorScheme.onSecondaryContainer
        message.isAgent2 -> MaterialTheme.colorScheme.onTertiaryContainer
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val icon = when {
        message.isUser -> Icons.Default.Person
        message.isAgent1 -> Icons.Default.Star
        message.isAgent2 -> Icons.Default.Search
        message.isError -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
    
    val label = when {
        message.isUser -> stringResource(R.string.user_label)
        message.isAgent1 -> "Agent 1"
        message.isAgent2 -> "Agent 2"
        message.isError -> "Error"
        else -> stringResource(R.string.ai_label)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            if (!message.isUser) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = label,
                fontSize = 12.sp,
                color = textColor,
            )
            
            if (message.isUser) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Card(
            modifier = Modifier
                .widthIn(max = if (message.isUser) 280.dp else 320.dp)
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ImageMessageItem(message: ChatMessage) {
    val backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    val textColor = MaterialTheme.colorScheme.onTertiaryContainer
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Image Generated",
                fontSize = 12.sp,
                color = textColor,
            )
        }
        
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Отображение изображения - безопасная загрузка через Coil
                message.imageUrl?.let { imagePath ->
                    // Показываем путь к изображению
                    Text(
                        text = "Image: $imagePath",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Безопасная загрузка изображения через Coil
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Generated image: ${message.imagePrompt}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var ollamaIp by remember { mutableStateOf("192.168.1.6") }
    var ollamaPort by remember { mutableStateOf("11434") }
    var hostIp by remember { mutableStateOf("192.168.1.6") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Ollama Server:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = ollamaIp,
                    onValueChange = { ollamaIp = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ollamaPort,
                    onValueChange = { ollamaPort = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "MCP Server (for images):",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it },
                    label = { Text("Host IP") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 For emulator: use 10.0.2.2\n💡 For real device: use your computer's IP",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun createRepository(): ChatRepository {
    val context = LocalContext.current
    
    // Создаем Retrofit для Ollama API
    val ollamaRetrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://192.168.1.6:11434/")
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
    val ollamaApi = ollamaRetrofit.create(com.tayrinn.aiadvent.data.api.OllamaApi::class.java)
    
    // Создаем Retrofit для Kandinsky API
    val kandinskyRetrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://192.168.1.6:8000/")
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
    val kandinskyApi = kandinskyRetrofit.create(com.tayrinn.aiadvent.data.api.KandinskyApi::class.java)
    
    // val chatMessageDao = com.tayrinn.aiadvent.data.database.ChatDatabase.getDatabase(context).chatMessageDao()
    val imageGenerationService = com.tayrinn.aiadvent.data.service.ImageGenerationService(
        kandinskyApi,
        context,
        com.tayrinn.aiadvent.data.preferences.ApiLimitsPreferences(context)
    )
    
    return ChatRepository(ollamaApi, /* chatMessageDao, */ imageGenerationService, context)
}
