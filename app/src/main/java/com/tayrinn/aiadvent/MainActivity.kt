package com.tayrinn.aiadvent

import android.os.Bundle
import android.util.Log
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
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen()
        }
    }
}
