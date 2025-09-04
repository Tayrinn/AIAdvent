package com.tayrinn.aiadvent.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayrinn.aiadvent.R
import com.tayrinn.aiadvent.auth.GoogleAuthResult
import com.tayrinn.aiadvent.auth.GoogleAuthService
import com.tayrinn.aiadvent.auth.GoogleUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (GoogleUser) -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authService = remember { GoogleAuthService(context) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Проверяем, авторизован ли уже пользователь
    LaunchedEffect(Unit) {
        if (authService.isUserSignedIn()) {
            val currentUser = authService.getCurrentUser()
            if (currentUser != null) {
                onAuthSuccess(currentUser)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Логотип приложения
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
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
            text = "AIAdvent",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Ваш умный AI помощник",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Кнопка входа через Google
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    
                    try {
                        val result = authService.signInWithGoogle()
                        
                        when {
                            result.isSuccess && result.user != null -> {
                                onAuthSuccess(result.user)
                            }
                            result.errorMessage != null -> {
                                errorMessage = result.errorMessage
                                onAuthError(result.errorMessage)
                            }
                            else -> {
                                val error = "Неизвестная ошибка авторизации"
                                errorMessage = error
                                onAuthError(error)
                            }
                        }
                    } catch (e: Exception) {
                        val error = "Ошибка: ${e.message}"
                        errorMessage = error
                        onAuthError(error)
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google иконка (текстовая версия)
                    Text(
                        text = "G",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "Войти через Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Сообщение об ошибке
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Информация о приложении
        Text(
            text = "Войдите в свой аккаунт Google, чтобы начать использовать все возможности AIAdvent",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AuthLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Проверяем авторизацию...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
