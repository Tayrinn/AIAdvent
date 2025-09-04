package com.tayrinn.aiadvent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tayrinn.aiadvent.auth.GoogleUser
import com.tayrinn.aiadvent.ui.auth.*
import com.tayrinn.aiadvent.ui.screens.ChatScreen
import com.tayrinn.aiadvent.ui.theme.AIAdventTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContent {
            AIAdventTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AIAdventApp()
                }
            }
        }
        Log.d(TAG, "onCreate completed")
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState called")
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState called")
    }
}

@Composable
fun AIAdventApp() {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))
    val authState by authViewModel.authState.collectAsState()
    
    when (authState) {
        is AuthState.Loading -> {
            AuthLoadingScreen()
        }
        
        is AuthState.Unauthenticated -> {
            AuthScreen(
                onAuthSuccess = { user ->
                    Log.d("MainActivity", "User authenticated: ${user.email}")
                },
                onAuthError = { error ->
                    Log.e("MainActivity", "Auth error: $error")
                }
            )
        }
        
        is AuthState.Authenticated -> {
            val user = (authState as AuthState.Authenticated).user
            Log.d("MainActivity", "Authenticated user: ${user.email}")
            
            // Основное приложение с навигацией
            MainAppContent(user = user, onSignOut = { authViewModel.signOut() })
        }
        
        is AuthState.Error -> {
            AuthScreen(
                onAuthSuccess = { user ->
                    Log.d("MainActivity", "User authenticated: ${user.email}")
                },
                onAuthError = { error ->
                    Log.e("MainActivity", "Auth error: $error")
                }
            )
        }
    }
}

@Composable
fun MainAppContent(
    user: GoogleUser,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                user = user,
                onSignOut = onSignOut
            )
        }
    }
}
