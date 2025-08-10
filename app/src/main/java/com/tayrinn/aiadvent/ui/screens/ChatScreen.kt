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
                title = { Text(stringResource(R.string.chat_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { showClearChatDialog = true }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_chat))
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
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
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
            title = { Text(stringResource(R.string.clear_chat_dialog_title)) },
            text = { Text(stringResource(R.string.clear_chat_dialog_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearChatDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
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
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else if (message.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else if (message.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = if (message.isUser) "Вы" else "AI",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
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
                    text = stringResource(R.string.typing_indicator),
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
            placeholder = { Text(stringResource(R.string.message_hint)) },
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
