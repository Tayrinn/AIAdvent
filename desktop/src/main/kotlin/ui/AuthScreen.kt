package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import auth.AuthResult
import auth.WebGoogleAuthService
import auth.DesktopUser
import kotlinx.coroutines.launch

@Composable
fun DesktopAuthScreen(
    authService: WebGoogleAuthService,
    onAuthSuccess: (DesktopUser) -> Unit,
    onAuthError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userCode by remember { mutableStateOf<String?>(null) }
    var verificationUri by remember { mutableStateOf<String?>(null) }
    var isWaitingForAuth by remember { mutableStateOf(false) }
    
    // Проверяем, авторизован ли уже пользователь
    LaunchedEffect(Unit) {
        val currentUser = authService.getCurrentUser()
        if (currentUser != null) {
            onAuthSuccess(currentUser)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Логотип приложения
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    fontSize = 48.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Название приложения
        Text(
            text = "AIAdvent Desktop",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Ваш умный AI помощник для рабочего стола",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Кнопка входа через Google
        Button(
            onClick = {
                scope.launch {
                    if (!isWaitingForAuth) {
                        // Начинаем Web OAuth авторизацию
                        isLoading = true
                        errorMessage = null
                        
                        scope.launch {
                            try {
                                val result = authService.signIn()
                                
                                if (result.success) {
                                    val user = authService.getCurrentUser()
                                    if (user != null) {
                                        onAuthSuccess(user)
                                    } else {
                                        val error = "Не удалось получить информацию о пользователе"
                                        errorMessage = error
                                        onAuthError(error)
                                    }
                                } else {
                                    val error = result.error ?: "Ошибка авторизации"
                                    errorMessage = error
                                    onAuthError(error)
                                }
                            } catch (e: Exception) {
                                val error = "Ошибка: ${e.message}"
                                errorMessage = error
                                onAuthError(error)
                            } finally {
                                isLoading = false
                                isWaitingForAuth = false
                            }
                        }
                    } else {
                        // Отменяем авторизацию
                        isWaitingForAuth = false
                        userCode = null
                        verificationUri = null
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .width(320.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Получение кода...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                isWaitingForAuth -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⏳",
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Отменить",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Google иконка (текстовая версия)
                        Text(
                            text = "G",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Войти через Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Информация о процессе авторизации
        Card(
            modifier = Modifier.width(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Безопасная авторизация",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "При нажатии на кнопку откроется браузер для входа в ваш аккаунт Google. После успешной авторизации вы автоматически вернетесь в приложение.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        
        // Код авторизации (если получен)
        if (isWaitingForAuth && userCode != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.width(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔑 Код авторизации",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Код авторизации крупным шрифтом
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = userCode ?: "",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 4.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Введите этот код на странице Google в браузере",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Ожидание подтверждения...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Сообщение об ошибке
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.width(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DesktopAuthLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Проверяем авторизацию...",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
