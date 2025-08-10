package com.tayrinn.aiadvent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tayrinn.aiadvent.ui.screens.ChatScreen
import com.tayrinn.aiadvent.ui.theme.AIAdventTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}

@Composable
fun AIAdventApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen()
        }
    }
}
