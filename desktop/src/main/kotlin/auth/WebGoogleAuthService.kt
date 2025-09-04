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
data class WebTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String,
    val id_token: String? = null
)

@Serializable
data class WebUserInfo(
    val id: String,
    val email: String,
    val name: String,
    val picture: String? = null
)

data class WebAuthResult(
    val success: Boolean,
    val error: String? = null
)

/**
 * Google OAuth сервис для Desktop приложений с использованием Web Flow
 */
class WebGoogleAuthService {
    
    companion object {
        private const val DEFAULT_CLIENT_ID = "764086051850-6qr4p6gpi6hn506pt8ejuq83di341hur.apps.googleusercontent.com"
        private const val SCOPE = "openid email profile"
        private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
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
    
    // Получаем Client Secret из конфигурации (обязательно для Web Flow)
    private val clientSecret: String? by lazy {
        try {
            val secret = com.tayrinn.aiadvent.util.ConfigService.getProperty("google.oauth.client.secret")
            if (secret.isNullOrBlank()) {
                println("❌ Google Client Secret не настроен (ОБЯЗАТЕЛЬНО для Web Flow)")
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
    
    fun getCurrentUser(): DesktopUser? = currentUser
    
    suspend fun signIn(): WebAuthResult = withContext(Dispatchers.IO) {
        try {
            println("🔐 Запуск Web OAuth авторизации...")
            
            if (clientSecret == null) {
                return@withContext WebAuthResult(false, "Client Secret не настроен")
            }
            
            // Генерируем state для безопасности
            val state = generateRandomString(32)
            
            // Строим URL авторизации
            val authUrl = buildAuthUrl(state)
            println("🌐 Открываем браузер: $authUrl")
            
            // Открываем браузер
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
            
            // Запускаем локальный сервер и ждем код
            val authCode = startLocalServerAndGetCode(state)
            
            if (authCode != null) {
                println("✅ Получен код авторизации")
                
                // Обмениваем код на токены
                val tokenResponse = exchangeCodeForTokens(authCode)
                if (tokenResponse != null) {
                    accessToken = tokenResponse.access_token
                    
                    // Получаем информацию о пользователе
                    val userInfo = getUserInfo()
                    if (userInfo != null) {
                        currentUser = DesktopUser(
                            id = userInfo.id,
                            email = userInfo.email,
                            name = userInfo.name,
                            picture = userInfo.picture
                        )
                        println("✅ Авторизация успешна: ${userInfo.name} (${userInfo.email})")
                        return@withContext WebAuthResult(true)
                    } else {
                        return@withContext WebAuthResult(false, "Не удалось получить информацию о пользователе")
                    }
                } else {
                    return@withContext WebAuthResult(false, "Не удалось обменять код на токены")
                }
            } else {
                return@withContext WebAuthResult(false, "Не получен код авторизации")
            }
        } catch (e: Exception) {
            println("❌ Ошибка авторизации: ${e.message}")
            e.printStackTrace()
            return@withContext WebAuthResult(false, "Ошибка авторизации: ${e.message}")
        }
    }
    
    fun signOut() {
        currentUser = null
        accessToken = null
        println("👋 Вы вышли из аккаунта")
    }
    
    private fun buildAuthUrl(state: String): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to REDIRECT_URI,
            "response_type" to "code",
            "scope" to SCOPE,
            "access_type" to "offline",
            "state" to state
        )
        
        val queryString = params.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
            .joinToString("&")
        
        return "$AUTH_URL?$queryString"
    }
    
    private suspend fun startLocalServerAndGetCode(expectedState: String): String? = suspendCoroutine { continuation ->
        try {
            val serverSocket = ServerSocket(LOCAL_SERVER_PORT)
            println("🔗 Локальный сервер запущен на порту $LOCAL_SERVER_PORT")
            
            // Таймаут для сервера
            serverSocket.soTimeout = 120000 // 2 минуты
            
            val socket = serverSocket.accept()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())
            
            // Читаем HTTP запрос
            val requestLine = reader.readLine()
            println("📥 Получен запрос: $requestLine")
            
            if (requestLine != null && requestLine.startsWith("GET /callback")) {
                val requestPath = requestLine.split(" ")[1]
                val url = URL("http://localhost:$LOCAL_SERVER_PORT$requestPath")
                val params = parseQueryParams(url.query)
                
                val code = params["code"]
                val state = params["state"]
                val error = params["error"]
                
                if (error != null) {
                    println("❌ Ошибка OAuth: $error")
                    sendErrorResponse(writer, "Ошибка авторизации: $error")
                    continuation.resume(null)
                } else if (code != null && state == expectedState) {
                    println("✅ Код авторизации получен")
                    sendSuccessResponse(writer)
                    continuation.resume(code)
                } else {
                    println("❌ Неверный код или state")
                    sendErrorResponse(writer, "Неверный код авторизации")
                    continuation.resume(null)
                }
            } else {
                println("❌ Неверный запрос")
                sendErrorResponse(writer, "Неверный запрос")
                continuation.resume(null)
            }
            
            socket.close()
            serverSocket.close()
            
        } catch (e: Exception) {
            println("❌ Ошибка локального сервера: ${e.message}")
            continuation.resume(null)
        }
    }
    
    private suspend fun exchangeCodeForTokens(code: String): WebTokenResponse? = withContext(Dispatchers.IO) {
        try {
            val requestBody = mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret!!,
                "code" to code,
                "grant_type" to "authorization_code",
                "redirect_uri" to REDIRECT_URI
            )
            
            val formBody = requestBody.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                .joinToString("&")
            
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            
            println("🔄 Обмениваем код на токены...")
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                println("✅ Токены получены успешно")
                json.decodeFromString<WebTokenResponse>(responseBody)
            } else {
                println("❌ Ошибка получения токенов: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка обмена кода на токены: ${e.message}")
            null
        }
    }
    
    private suspend fun getUserInfo(): WebUserInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(USER_INFO_URL)
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                json.decodeFromString<WebUserInfo>(responseBody)
            } else {
                println("❌ Ошибка получения информации о пользователе: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка получения информации о пользователе: ${e.message}")
            null
        }
    }
    
    private fun parseQueryParams(query: String?): Map<String, String> {
        if (query == null) return emptyMap()
        
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }
            .toMap()
    }
    
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    private fun sendSuccessResponse(writer: OutputStreamWriter) {
        val response = """
            HTTP/1.1 200 OK
            Content-Type: text/html; charset=utf-8
            
            <!DOCTYPE html>
            <html>
            <head>
                <title>Авторизация успешна</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    .success { color: green; font-size: 24px; }
                </style>
            </head>
            <body>
                <div class="success">✅ Авторизация успешна!</div>
                <p>Вы можете закрыть это окно и вернуться к приложению.</p>
            </body>
            </html>
        """.trimIndent()
        
        writer.write(response)
        writer.flush()
    }
    
    private fun sendErrorResponse(writer: OutputStreamWriter, error: String) {
        val response = """
            HTTP/1.1 400 Bad Request
            Content-Type: text/html; charset=utf-8
            
            <!DOCTYPE html>
            <html>
            <head>
                <title>Ошибка авторизации</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    .error { color: red; font-size: 24px; }
                </style>
            </head>
            <body>
                <div class="error">❌ Ошибка авторизации</div>
                <p>$error</p>
                <p>Попробуйте еще раз или обратитесь к администратору.</p>
            </body>
            </html>
        """.trimIndent()
        
        writer.write(response)
        writer.flush()
    }
}
