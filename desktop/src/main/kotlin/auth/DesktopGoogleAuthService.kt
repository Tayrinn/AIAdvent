package auth

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.Desktop
import java.io.*
import java.net.*
import java.security.SecureRandom
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val verification_uri_complete: String? = null,
    val expires_in: Int,
    val interval: Int? = null
)

@Serializable
data class GoogleTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String,
    val id_token: String? = null
)

@Serializable
data class TokenErrorResponse(
    val error: String,
    val error_description: String? = null
)

@Serializable
data class GoogleUserInfo(
    val id: String,
    val email: String,
    val name: String,
    val picture: String? = null
)

data class DesktopUser(
    val id: String,
    val email: String,
    val name: String,
    val picture: String?
)

data class AuthResult(
    val success: Boolean,
    val user: DesktopUser? = null,
    val error: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null
)

class DesktopGoogleAuthService {
    
    companion object {
        // Временный тестовый Desktop Client ID для Device Flow
        // ВАЖНО: Замените на ваш собственный Desktop Client ID в production
        private const val DEFAULT_CLIENT_ID = "764086051850-6qr4p6gpi6hn506pt8ejuq83di341hur.apps.googleusercontent.com"
        private const val SCOPE = "openid email profile"
        private const val DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
        
        // Web OAuth Flow constants
        private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val REDIRECT_URI = "http://localhost:8080/callback"
        private const val LOCAL_SERVER_PORT = 8080
    }
    
    // Получаем Client ID из конфигурации или используем по умолчанию
    private val clientId: String by lazy {
        try {
            val configId = com.tayrinn.aiadvent.util.ConfigService.getProperty("google.oauth.client.id")
            if (configId.isNullOrBlank()) {
                println("⚠️ Google Client ID не настроен в конфигурации, используем по умолчанию")
                DEFAULT_CLIENT_ID
            } else {
                println("✅ Используем Google Client ID из конфигурации")
                configId
            }
        } catch (e: Exception) {
            println("⚠️ Не удалось загрузить Google Client ID из конфигурации: ${e.message}")
            DEFAULT_CLIENT_ID
        }
    }
    
    // Получаем Client Secret из конфигурации (опционально для Device Flow)
    private val clientSecret: String? by lazy {
        try {
            val secret = com.tayrinn.aiadvent.util.ConfigService.getProperty("google.oauth.client.secret")
            if (secret.isNullOrBlank()) {
                println("⚠️ Google Client Secret не настроен (может не потребоваться для Device Flow)")
                null
            } else {
                println("✅ Используем Google Client Secret из конфигурации")
                secret
            }
        } catch (e: Exception) {
            println("⚠️ Не удалось загрузить Google Client Secret: ${e.message}")
            null
        }
    }
    
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    private var currentUser: DesktopUser? = null
    private var accessToken: String? = null
    
    fun isSignedIn(): Boolean = currentUser != null
    
    fun getCurrentUser(): DesktopUser? = currentUser
    
    suspend fun startSignIn(): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Шаг 1: Получаем device code
            val deviceCodeResponse = getDeviceCode()
            if (deviceCodeResponse == null) {
                return@withContext AuthResult(false, error = "Ошибка получения device code")
            }
            
            // Шаг 2: Открываем браузер с URL для авторизации
            val authUrl = deviceCodeResponse.verification_uri_complete ?: deviceCodeResponse.verification_uri
            
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
            
            println("🔐 Откроется браузер для входа в Google")
            println("📱 Введите код: ${deviceCodeResponse.user_code}")
            println("🌐 Или перейдите по ссылке: $authUrl")
            
            // Возвращаем информацию для UI
            return@withContext AuthResult(
                success = false, // Пока не завершено
                userCode = deviceCodeResponse.user_code,
                verificationUri = authUrl
            )
            
        } catch (e: Exception) {
            println("❌ Ошибка начала авторизации: ${e.message}")
            e.printStackTrace()
            return@withContext AuthResult(false, error = "Ошибка авторизации: ${e.message}")
        }
    }
    
    suspend fun completeSignIn(deviceCode: String, interval: Int, expiresIn: Int): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Шаг 3: Polling для получения токенов
            val tokenResponse = pollForTokens(
                deviceCode = deviceCode,
                interval = interval,
                expiresIn = expiresIn
            )
            
            if (tokenResponse == null) {
                return@withContext AuthResult(false, error = "Авторизация отменена или истекло время ожидания")
            }
            
            accessToken = tokenResponse.access_token
            
            // Шаг 4: Получаем информацию о пользователе
            val userInfo = getUserInfo(tokenResponse.access_token)
            
            if (userInfo == null) {
                return@withContext AuthResult(false, error = "Ошибка получения информации о пользователе")
            }
            
            currentUser = DesktopUser(
                id = userInfo.id,
                email = userInfo.email,
                name = userInfo.name,
                picture = userInfo.picture
            )
            
            return@withContext AuthResult(true, currentUser)
            
        } catch (e: Exception) {
            println("❌ Ошибка завершения авторизации: ${e.message}")
            e.printStackTrace()
            return@withContext AuthResult(false, error = "Ошибка авторизации: ${e.message}")
        }
    }
    
    // Упрощенный метод для обратной совместимости
    suspend fun signIn(): AuthResult {
        val startResult = startSignIn()
        if (!startResult.success && startResult.userCode != null) {
            // Получаем device code для завершения
            val deviceCodeResponse = getDeviceCode()
            if (deviceCodeResponse != null) {
                return completeSignIn(
                    deviceCode = deviceCodeResponse.device_code,
                    interval = deviceCodeResponse.interval ?: 5,
                    expiresIn = deviceCodeResponse.expires_in
                )
            }
        }
        return startResult
    }
    
    fun signOut() {
        currentUser = null
        accessToken = null
        println("👋 Вы вышли из аккаунта")
    }
    
    suspend fun getDeviceCode(): DeviceCodeResponse? = withContext(Dispatchers.IO) {
        try {
            val requestBody = mutableMapOf(
                "client_id" to clientId,
                "scope" to SCOPE
            )
            
            // Добавляем Client Secret, если доступен
            clientSecret?.let { secret ->
                requestBody["client_secret"] = secret
            }
            
            val formBody = requestBody.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                .joinToString("&")
            
            val request = Request.Builder()
                .url(DEVICE_CODE_URL)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            
            println("🔍 Device Code Request:")
            println("   Client ID: ${clientId.take(20)}...${clientId.takeLast(10)}")
            println("   Client Secret: ${if (clientSecret != null) "*** (configured)" else "не настроен"}")
            println("   Request Body: ${requestBody.keys}")
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                println("✅ Device code получен успешно")
                json.decodeFromString<DeviceCodeResponse>(responseBody)
            } else {
                println("❌ Ошибка получения device code: ${response.code} - $responseBody")
                
                // Дополнительная диагностика
                when (response.code) {
                    401 -> println("💡 Проверьте правильность Client ID и Client Secret")
                    400 -> println("💡 Проверьте формат запроса и обязательные параметры")
                    403 -> println("💡 Проверьте, включен ли Device Flow API в Google Console")
                }
                
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка получения device code: ${e.message}")
            null
        }
    }
    
    private suspend fun pollForTokens(
        deviceCode: String,
        interval: Int,
        expiresIn: Int
    ): GoogleTokenResponse? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = expiresIn * 1000L
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val requestBody = mutableMapOf(
                    "client_id" to clientId,
                    "device_code" to deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                )
                
                // Добавляем Client Secret, если доступен
                clientSecret?.let { secret ->
                    requestBody["client_secret"] = secret
                }
                
                val formBody = requestBody.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                    .joinToString("&")
                
                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    // Успешная авторизация
                    return@withContext json.decodeFromString<GoogleTokenResponse>(responseBody)
                } else if (responseBody != null) {
                    // Проверяем тип ошибки
                    try {
                        val errorResponse = json.decodeFromString<TokenErrorResponse>(responseBody)
                        when (errorResponse.error) {
                            "authorization_pending" -> {
                                // Пользователь еще не завершил авторизацию - продолжаем ждать
                                println("⏳ Ожидание авторизации пользователя...")
                            }
                            "slow_down" -> {
                                // Нужно увеличить интервал опроса
                                println("🐌 Замедляем опрос...")
                                delay((interval + 5) * 1000L)
                                continue
                            }
                            "expired_token" -> {
                                println("❌ Время авторизации истекло")
                                return@withContext null
                            }
                            "access_denied" -> {
                                println("❌ Авторизация отклонена пользователем")
                                return@withContext null
                            }
                            else -> {
                                println("❌ Ошибка авторизации: ${errorResponse.error} - ${errorResponse.error_description}")
                                return@withContext null
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Неизвестная ошибка: ${response.code} - $responseBody")
                        return@withContext null
                    }
                }
                
                // Ждем перед следующим запросом
                delay(interval * 1000L)
                
            } catch (e: Exception) {
                println("❌ Ошибка опроса токенов: ${e.message}")
                delay(interval * 1000L)
            }
        }
        
        println("❌ Время ожидания авторизации истекло")
        return@withContext null
    }
    
    private suspend fun getUserInfo(accessToken: String): GoogleUserInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(USER_INFO_URL)
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                json.decodeFromString<GoogleUserInfo>(responseBody)
            } else {
                println("❌ Ошибка получения информации о пользователе: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка получения информации о пользователе: ${e.message}")
            null
        }
    }
    
}
