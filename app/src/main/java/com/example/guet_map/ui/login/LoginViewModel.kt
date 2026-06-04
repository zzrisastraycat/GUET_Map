package com.example.guet_map.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val userId: String = "",
    val points: Int = 0,
    val loading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(refreshState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = refreshState()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            authRepository.login(username, password).collect { resource ->
                when (resource) {
                    is Resource.Loading ->
                        _uiState.value = _uiState.value.copy(loading = true, message = null)
                    is Resource.Success -> {
                        _uiState.value = refreshState().copy(
                            loading = false,
                            message = "欢迎，${resource.data.nickname}"
                        )
                    }
                    is Resource.Error ->
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            message = resource.message
                        )
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = refreshState().copy(message = "已退出登录")
    }

    private fun refreshState(): LoginUiState = LoginUiState(
        isLoggedIn = authRepository.isLoggedIn,
        nickname = authRepository.nickname,
        userId = authRepository.userId,
        points = userPrefs.points
    )
}
