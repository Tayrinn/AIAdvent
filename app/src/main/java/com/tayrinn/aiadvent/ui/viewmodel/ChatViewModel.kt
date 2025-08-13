package com.tayrinn.aiadvent.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayrinn.aiadvent.data.model.ChatMessage
import com.tayrinn.aiadvent.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            repository.getAllMessages().collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun setApiKey(apiKey: String) {
        _apiKey.value = apiKey
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val userMessage = ChatMessage(
            content = content,
            isUser = true
        )
        
        viewModelScope.launch {
            // Сохраняем сообщение пользователя
            repository.insertMessage(userMessage)
            
            // Отправляем сообщение в Ollama
            _isLoading.value = true
            
            try {
                val (agent1Response, agent2Response) = repository.sendMessage(
                    content,
                    _messages.value
                )
                
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

    fun clearChat() {
        viewModelScope.launch {
            repository.deleteAllMessages()
        }
    }
}
