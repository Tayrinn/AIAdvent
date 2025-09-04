package com.tayrinn.aiadvent.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class GoogleAuthResult(
    val isSuccess: Boolean,
    val user: GoogleUser? = null,
    val errorMessage: String? = null
)

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

class GoogleAuthService(private val context: Context) {
    
    companion object {
        private const val CLIENT_ID = "95693732704-lcjviftb0ldbs839t59vhihv2qk4j7po.apps.googleusercontent.com"
    }
    
    private val credentialManager = CredentialManager.create(context)
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }
    
    /**
     * Получает текущего авторизованного пользователя
     */
    fun getCurrentUser(): GoogleUser? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return GoogleUser(
            id = account.id ?: "",
            email = account.email ?: "",
            displayName = account.displayName ?: "",
            photoUrl = account.photoUrl?.toString()
        )
    }
    
    /**
     * Авторизация через Google (новый Credential Manager API)
     */
    suspend fun signInWithGoogle(): GoogleAuthResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(CLIENT_ID)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            
            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                val user = GoogleUser(
                    id = credential.id,
                    email = credential.id, // В новом API id это email
                    displayName = credential.displayName ?: "",
                    photoUrl = credential.profilePictureUri?.toString()
                )
                
                GoogleAuthResult(
                    isSuccess = true,
                    user = user
                )
            } else {
                GoogleAuthResult(
                    isSuccess = false,
                    errorMessage = "Неподдерживаемый тип credential"
                )
            }
        } catch (e: GetCredentialException) {
            GoogleAuthResult(
                isSuccess = false,
                errorMessage = "Ошибка авторизации: ${e.message}"
            )
        } catch (e: Exception) {
            GoogleAuthResult(
                isSuccess = false,
                errorMessage = "Неожиданная ошибка: ${e.message}"
            )
        }
    }
    
    /**
     * Авторизация через Google (старый API для совместимости)
     */
    suspend fun signInWithGoogleLegacy(): GoogleAuthResult {
        return suspendCoroutine { continuation ->
            try {
                val signInIntent = googleSignInClient.signInIntent
                // Этот метод требует ActivityResultLauncher, поэтому используем новый API выше
                continuation.resume(
                    GoogleAuthResult(
                        isSuccess = false,
                        errorMessage = "Используйте signInWithGoogle() вместо legacy метода"
                    )
                )
            } catch (e: Exception) {
                continuation.resume(
                    GoogleAuthResult(
                        isSuccess = false,
                        errorMessage = "Ошибка запуска авторизации: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Выход из аккаунта
     */
    suspend fun signOut(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }
    
    /**
     * Полное отключение аккаунта
     */
    suspend fun revokeAccess(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            googleSignInClient.revokeAccess().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }
}
