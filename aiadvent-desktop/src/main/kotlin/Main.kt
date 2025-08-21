import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = "AI Advent Desktop App",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Список сообщений
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
            }
            
            // Поле ввода
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Введите сообщение...") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMessage = ChatMessage(
                                content = inputText,
                                isUser = true
                            )
                            messages = messages + userMessage
                            
                            scope.launch {
                                isLoading = true
                                try {
                                    when {
                                        inputText.lowercase().contains("run tests") -> {
                                            val testRunner = TestRunner()
                                            val testReport = testRunner.runTests()
                                            val testMessage = ChatMessage(
                                                content = testReport.getSummary(),
                                                isUser = false,
                                                isTestReport = true
                                            )
                                            messages = messages + testMessage
                                        }
                                        inputText.lowercase().contains("generate") || 
                                        inputText.lowercase().contains("create") || 
                                        inputText.lowercase().contains("make") || 
                                        inputText.lowercase().contains("draw") || 
                                        inputText.lowercase().contains("picture") || 
                                        inputText.lowercase().contains("image") -> {
                                            val response = ChatMessage(
                                                content = "🎨 Генерация изображения: $inputText\n\n(В desktop версии генерация изображений не поддерживается)",
                                                isUser = false,
                                                isImageGeneration = true
                                            )
                                            messages = messages + response
                                        }
                                        else -> {
                                            val agent1Response = ChatMessage(
                                                content = "🤖 **Agent 1:** Это ответ от первого агента на ваше сообщение: $inputText",
                                                isUser = false,
                                                isAgent1 = true
                                            )
                                            val agent2Response = ChatMessage(
                                                content = "🔍 **Agent 2 (Enhancement):** Это улучшенный ответ от второго агента: $inputText",
                                                isUser = false,
                                                isAgent2 = true
                                            )
                                            messages = messages + agent1Response + agent2Response
                                        }
                                    }
                                } catch (e: Exception) {
                                    val errorMessage = ChatMessage(
                                        content = "Error: ${e.message}",
                                        isUser = false,
                                        isError = true
                                    )
                                    messages = messages + errorMessage
                                } finally {
                                    isLoading = false
                                }
                            }
                            
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Отправить")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val backgroundColor = when {
        message.isUser -> MaterialTheme.colors.primary.copy(alpha = 0.1f)
        message.isError -> MaterialTheme.colors.error.copy(alpha = 0.1f)
        message.isTestReport -> MaterialTheme.colors.secondary.copy(alpha = 0.1f)
        message.isImageGeneration -> MaterialTheme.colors.primaryVariant.copy(alpha = 0.1f)
        else -> MaterialTheme.colors.surface
    }
    
    val borderColor = when {
        message.isUser -> MaterialTheme.colors.primary
        message.isError -> MaterialTheme.colors.error
        message.isTestReport -> MaterialTheme.colors.secondary
        message.isImageGeneration -> MaterialTheme.colors.primaryVariant
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        backgroundColor = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (message.isUser) "Вы" else "AI",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI Advent Desktop"
    ) {
        App()
    }
}
