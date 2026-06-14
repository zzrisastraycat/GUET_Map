package com.example.guet_map.module.ai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.domain.usecase.GetChatHistoryUseCase
import com.example.guet_map.module.ai.domain.usecase.SendMessageUseCase
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * AI 对话 ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 对话会话 ID
    private val sessionId = UUID.randomUUID().toString()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            getChatHistoryUseCase(sessionId).collect { history ->
                _messages.value = history
                _uiState.value = if (history.isEmpty()) {
                    ChatUiState.Empty
                } else {
                    ChatUiState.Success(history)
                }
            }
        }
    }

    fun sendMessage(content: String, locationId: String? = null) {
        if (content.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            when (val result = sendMessageUseCase(sessionId, content, locationId)) {
                is Resource.Success -> {
                    // 消息已通过 Flow 更新
                }
                is Resource.Error -> {
                    _uiState.value = ChatUiState.Error(result.message)
                }
                is Resource.Loading -> {
                    // 已在加载中
                }
            }

            _isLoading.value = false
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            _messages.value = emptyList()
            _uiState.value = ChatUiState.Empty
        }
    }
}

sealed class ChatUiState {
    data object Loading : ChatUiState()
    data object Empty : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
