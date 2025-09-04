package com.tayrinn.aiadvent.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tayrinn.aiadvent.auth.GoogleAuthService
import com.tayrinn.aiadvent.auth.GoogleUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: GoogleUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authService: GoogleAuthService) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                if (authService.isUserSignedIn()) {
                    val user = authService.getCurrentUser()
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка проверки авторизации: ${e.message}")
            }
        }
    }
    
    fun signIn() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            try {
                val result = authService.signInWithGoogle()
                
                when {
                    result.isSuccess && result.user != null -> {
                        _authState.value = AuthState.Authenticated(result.user)
                    }
                    result.errorMessage != null -> {
                        _authState.value = AuthState.Error(result.errorMessage)
                    }
                    else -> {
                        _authState.value = AuthState.Error("Неизвестная ошибка авторизации")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка авторизации: ${e.message}")
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                val success = authService.signOut()
                if (success) {
                    _authState.value = AuthState.Unauthenticated
                } else {
                    _authState.value = AuthState.Error("Ошибка выхода из аккаунта")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка выхода: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(GoogleAuthService(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
