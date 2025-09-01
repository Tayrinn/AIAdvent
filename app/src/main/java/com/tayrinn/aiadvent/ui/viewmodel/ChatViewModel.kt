package com.tayrinn.aiadvent.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.model.ApiLimits
import com.tayrinn.aiadvent.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
                    private val _isGeneratingImage = MutableStateFlow(false)
                val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

                private val _apiLimits = MutableStateFlow<ApiLimits?>(null)
                val apiLimits: StateFlow<ApiLimits?> = _apiLimits.asStateFlow()
    
    init {
        Log.d(TAG, "ChatViewModel init called")
        // ПРИНУДИТЕЛЬНО ОЧИЩАЕМ БД ПРИ ПЕРВОМ ЗАПУСКЕ
        viewModelScope.launch {
            try {
                Log.d(TAG, "FORCING DATABASE CLEAR ON FIRST LAUNCH")
                repository.clearMessages()
                Log.d(TAG, "Database cleared successfully on first launch")
                
                // Добавляем тестовое сообщение
                val testMessage = ChatMessage(
                    content = "🚀 **FIRST LAUNCH TEST:** Database cleared and ready!",
                    isUser = false,
                    isAgent1 = true
                )
                repository.insertMessage(testMessage)
                Log.d(TAG, "Test message added after DB clear")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing DB on first launch: ${e.message}")
            }
        }
        
                            // ПОДПИСЫВАЕМСЯ НА FLOW ДЛЯ АВТОМАТИЧЕСКИХ ОБНОВЛЕНИЙ
                    viewModelScope.launch {
                        repository.getAllMessages().collect { messages ->
                            Log.d(TAG, "Flow update received: ${messages.size} messages")
                            _messages.value = messages
                        }
                    }
                    
                    // ЗАГРУЖАЕМ ЛИМИТЫ API
                    viewModelScope.launch {
                        loadApiLimits()
                    }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel onCleared called")
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val userMessage = ChatMessage(
            content = content,
            isUser = true
        )
        
        viewModelScope.launch {
            try {
                repository.insertMessage(userMessage)
                _isLoading.value = true
                
                // Проверяем специальные команды
                when {
                    content.lowercase().contains("run tests") -> {
                        runTests()
                    }
                    isImageGenerationRequest(content) -> {
                        generateImage(content)
                    }
                    else -> {
                        // Обычный текстовый ответ без сложных таймаутов
                        val (agent1Response, agent2Response) = repository.sendMessage(content, _messages.value, null)
                        
                        // Сохраняем ответ Агента 1
                        val agent1Message = ChatMessage(
                            content = "🤖 **Agent 1:** $agent1Response",
                            isUser = false,
                            isAgent1 = true
                        )
                        repository.insertMessage(agent1Message)
                        
                        // Сохраняем ответ Агента 2
                        val agent2Message = ChatMessage(
                            content = "🔍 **Agent 2 (Enhancement):** $agent2Response",
                            isUser = false,
                            isAgent2 = true
                        )
                        repository.insertMessage(agent2Message)
                    }
                }
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Error: ${e.message}",
                    isUser = false,
                    isError = true
                )
                repository.insertMessage(errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun runTests() {
        Log.d(TAG, "Running tests...")
        
        try {
            // Создаем TestRunner
            val testRunner = com.tayrinn.aiadvent.util.TestRunner(repository.getContext())
            
            // Запускаем тесты
            val testReport = testRunner.runTests()
            
            // Сохраняем отчет о тестах
            val testMessage = ChatMessage(
                content = testReport.getSummary(),
                isUser = false,
                isTestReport = true
            )
            repository.insertMessage(testMessage)
            
            Log.d(TAG, "Tests completed: ${testReport.passedTests} passed, ${testReport.failedTests} failed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running tests: ${e.message}")
            val errorMessage = ChatMessage(
                content = "Error running tests: ${e.message}",
                isUser = false,
                isError = true
            )
            repository.insertMessage(errorMessage)
        }
    }
    
    private suspend fun generateImage(prompt: String) {
        Log.d("ChatViewModel", "generateImage - Current thread: ${Thread.currentThread().name}")
        _isGeneratingImage.value = true
        
        try {
            // Извлекаем промпт для изображения
            val imagePrompt = extractImagePrompt(prompt)
            Log.d("ChatViewModel", "Extracted prompt: $imagePrompt")
            
            // Генерируем изображение в отдельной корутине
            Log.d("ChatViewModel", "Calling repository.generateImage...")
            val result = withContext(Dispatchers.IO) {
                repository.generateImage(imagePrompt)
            }
            Log.d("ChatViewModel", "Repository result received")
            
                                    result.fold(
                            onSuccess = { imagePath ->
                                Log.d("ChatViewModel", "Image generated successfully: $imagePath")
                                // Уменьшаем счетчик лимитов
                                repository.decreaseApiLimits()
                                // Обновляем лимиты
                                loadApiLimits()
                                
                                // Сохраняем сообщение с изображением
                                val imageMessage = ChatMessage(
                                    content = "🎨 **Generated Image:** $imagePrompt",
                                    isUser = false,
                                    isImageGeneration = true,
                                    imageUrl = imagePath,
                                    imagePrompt = imagePrompt
                                )
                                repository.insertMessage(imageMessage)
                            },
                onFailure = { exception ->
                    Log.e("ChatViewModel", "Image generation failed: ${exception.message}")
                    val errorMessage = ChatMessage(
                        content = "Error generating image: ${exception.message}",
                        isUser = false,
                        isError = true
                    )
                    repository.insertMessage(errorMessage)
                }
            )
            
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Exception in generateImage: ${e.message}")
            val errorMessage = ChatMessage(
                content = "Error: ${e.message}",
                isUser = false,
                isError = true
            )
            repository.insertMessage(errorMessage)
        } finally {
            _isGeneratingImage.value = false
            Log.d("ChatViewModel", "generateImage completed")
        }
    }
    
    private fun isImageGenerationRequest(content: String): Boolean {
        val lowerContent = content.lowercase()
        return lowerContent.contains("сгенерируй") && 
               (lowerContent.contains("изображение") || lowerContent.contains("картинку") || lowerContent.contains("рисунок")) ||
               lowerContent.contains("generate") && 
               (lowerContent.contains("image") || lowerContent.contains("picture") || lowerContent.contains("drawing"))
    }
    
    private fun extractImagePrompt(content: String): String {
        // Убираем команды типа "сгенерируй изображение" и оставляем только описание
        val lowerContent = content.lowercase()
        
        return when {
            lowerContent.contains("сгенерируй изображение") -> 
                content.replace("сгенерируй изображение", "").trim()
            lowerContent.contains("сгенерируй картинку") -> 
                content.replace("сгенерируй картинку", "").trim()
            lowerContent.contains("сгенерируй рисунок") -> 
                content.replace("сгенерируй рисунок", "").trim()
            lowerContent.contains("generate image") -> 
                content.replace("generate image", "").trim()
            lowerContent.contains("generate picture") -> 
                content.replace("generate picture", "").trim()
            lowerContent.contains("generate drawing") -> 
                content.replace("generate drawing", "").trim()
            else -> content.trim()
        }
    }
    
    fun loadMessages() {
        Log.d(TAG, "loadMessages called - Flow subscription already active")
        // Flow подписка уже активна в init, дополнительная загрузка не нужна
    }
    
    fun clearMessages() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "clearMessages called")
                repository.clearMessages()
                Log.d(TAG, "Messages cleared successfully")

                // Добавляем тестовое сообщение для проверки отображения
                val testMessage = ChatMessage(
                    content = "🧪 **Test Message:** This is a test message to verify UI rendering works correctly!",
                    isUser = false,
                    isAgent1 = true
                )
                repository.insertMessage(testMessage)
                Log.d(TAG, "Test message added successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error clearing messages: ${e.message}")
            }
        }
    }
    
                    fun cancelImageGeneration() {
                    // Просто сбрасываем флаги - операция завершится по таймауту
                    _isGeneratingImage.value = false
                    _isLoading.value = false
                }
                
                private suspend fun loadApiLimits() {
                    try {
                        Log.d(TAG, "Loading API limits...")
                        val limits = repository.getApiLimits()
                        _apiLimits.value = limits
                        Log.d(TAG, "API limits loaded: ${limits?.remainingGenerations}/${limits?.totalGenerations}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load API limits: ${e.message}")
                    }
                }
                
                fun refreshApiLimits() {
                    viewModelScope.launch {
                        loadApiLimits()
                    }
                }
}
