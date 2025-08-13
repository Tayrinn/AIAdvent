package com.tayrinn.aiadvent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.tayrinn.aiadvent.R
import org.json.JSONObject
import org.json.JSONArray

fun cleanJsonString(raw: String): String {
    var s = raw.trim()
    
    // Заменяем одинарные кавычки на двойные
    s = s.replace("'", "\"")
    
    // Вырезаем всё до первой { или [ и после последней } или ]
    val start = minOf(
        s.indexOf('{').let { if (it >= 0) it else Int.MAX_VALUE },
        s.indexOf('[').let { if (it >= 0) it else Int.MAX_VALUE }
    )
    val end = maxOf(
        s.lastIndexOf('}').let { if (it >= 0) it else -1 },
        s.lastIndexOf(']').let { if (it >= 0) it else -1 }
    )
    
    if (start != Int.MAX_VALUE && end >= start) {
        s = s.substring(start, end + 1)
    }
    
    // Исправляем типичные ошибки
    s = s.replace(",\n", ",")  // Убираем переносы строк после запятых
    s = s.replace(",\r", ",")  // Убираем возврат каретки после запятых
    s = s.replace(",\t", ",")  // Убираем табуляцию после запятых
    s = s.replace(", ", ",")   // Убираем пробелы после запятых
    
    // Исправляем незакрытые кавычки
    var quoteCount = 0
    for (char in s) {
        if (char == '"') quoteCount++
    }
    if (quoteCount % 2 != 0) {
        s += "\""
    }
    
    // Пытаемся исправить незакрытые скобки
    var openBraces = 0
    var openBrackets = 0
    
    for (char in s) {
        when (char) {
            '{' -> openBraces++
            '}' -> openBraces--
            '[' -> openBrackets++
            ']' -> openBrackets--
        }
    }
    
    // Добавляем недостающие закрывающие скобки
    repeat(openBraces) { s += "}" }
    repeat(openBrackets) { s += "]" }
    
    return s.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { showClearChatDialog = true }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.menu_clear_chat))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Список сообщений
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageItem(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            // Поле ввода сообщения
            MessageInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                        scope.launch {
                            focusRequester.requestFocus()
                        }
                    }
                },
                isLoading = isLoading,
                focusRequester = focusRequester
            )
        }
    }

    // Диалог для ввода API ключа
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentApiKey = apiKey,
            onApiKeyChange = { newApiKey ->
                viewModel.setApiKey(newApiKey)
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    // Диалог подтверждения очистки чата
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text(stringResource(R.string.menu_clear_chat)) },
            text = { Text("") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearChat()
                    showClearChatDialog = false
                }) {
                    Text(stringResource(R.string.menu_clear_chat))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
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
                .widthIn(max = 280.dp)
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
fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.loading),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.chat_hint)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { onSendClick() }
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        FloatingActionButton(
            onClick = onSendClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = stringResource(R.string.send),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyDialog(
    currentApiKey: String,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentApiKey) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.api_key_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.api_key_dialog_description),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    stringResource(R.string.api_key_help),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApiKeyChange(apiKey)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun JsonTable(json: String) {
    var error: String? by remember { mutableStateOf(null) }
    var jsonObject: JSONObject? = null
    var jsonArray: JSONArray? = null
    var isValidJson by remember { mutableStateOf(false) }

    val cleaned = cleanJsonString(json)

    try {
        if (cleaned.trim().startsWith("{")) {
            jsonObject = JSONObject(cleaned)
            isValidJson = true
        } else if (cleaned.trim().startsWith("[")) {
            jsonArray = JSONArray(cleaned)
            isValidJson = true
        } else {
            error = "Could not recognize JSON format"
        }
    } catch (e: Exception) {
        error = "JSON parsing error: ${e.localizedMessage}"
    }

    if (error != null) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "📝 AI Response:",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = json,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
        }
        return
    }

    if (jsonObject != null) {
        val keyCount = jsonObject.length()
        Text(
            "📊 Structured Data ($keyCount fields):",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        JsonObjectTable(jsonObject)
    } else if (jsonArray != null) {
        val arrayLength = jsonArray.length()
        Text(
            "📋 List ($arrayLength items):",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        JsonArrayTable(jsonArray)
    }
}

@Composable
fun JsonObjectTable(obj: JSONObject) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        var isFirst = true
        for (key in obj.keys()) {
            if (!isFirst) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
            isFirst = false
            
            val value = obj.get(key)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(0.4f)
                )
                if (value is JSONObject) {
                    JsonObjectTable(value)
                } else if (value is JSONArray) {
                    JsonArrayTable(value)
                } else {
                    Text(
                        text = value.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun JsonArrayTable(array: JSONArray) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
            .heightIn(max = 400.dp)
    ) {
        items(array.length()) { i ->
            val value = array.get(i)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "•",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (value is JSONObject) {
                    JsonObjectTable(value)
                } else if (value is JSONArray) {
                    JsonArrayTable(value)
                } else {
                    Text(
                        text = value.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (i < array.length() - 1) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}
