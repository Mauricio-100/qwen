package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: CmoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(user: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            try {
                val response = repository.apiService.login(user, pass)
                repository.prefs.saveAuth(response.accessToken, response.userId, response.username)
                repository.connectWebSocket()
                _uiState.value = AuthState.Success
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(user: String, pass: String, email: String) {
         viewModelScope.launch {
            _uiState.value = AuthState.Loading
            try {
                repository.apiService.register(mapOf(
                    "username" to user,
                    "password" to pass,
                    "email" to email
                ))
                _uiState.value = AuthState.RegistrationSuccess
            } catch (e: Exception) {
                _uiState.value = AuthState.Error("Registration failed")
            }
         }
    }

    fun logout() {
        viewModelScope.launch {
            repository.prefs.clearAuth()
            repository.disconnectWebSocket()
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object RegistrationSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}
