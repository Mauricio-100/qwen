package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.User
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ProfileViewModel(private val repository: CmoRepository) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _myVideos = MutableStateFlow<List<com.example.data.models.Video>>(emptyList())
    val myVideos: StateFlow<List<com.example.data.models.Video>> = _myVideos.asStateFlow()

    private val _serverStats = MutableStateFlow<Map<String, Any>?>(null)
    val serverStats = _serverStats.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _verificationStatus = MutableStateFlow<com.example.data.models.VerificationStatusResponse?>(null)
    val verificationStatus: StateFlow<com.example.data.models.VerificationStatusResponse?> = _verificationStatus.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                _allUsers.value = repository.apiService.getUsers()
            } catch (e: Exception) {}
        }
    }

    fun loadServerStats() {
        viewModelScope.launch {
            try {
                _serverStats.value = repository.apiService.getServerStats()
            } catch (e: Exception) {}
        }
    }

    fun loadMyVideos() {
        viewModelScope.launch {
            try {
                val me = repository.prefs.userIdFlow.firstOrNull() ?: ""
                if (me.isNotEmpty()) {
                    _myVideos.value = repository.apiService.getUserVideos(me)
                }
            } catch (e: Exception) {}
        }
    }

    fun updateProfile(updates: Map<String, String>) {
         viewModelScope.launch {
             try {
                 _userProfile.value = repository.apiService.updateProfile(updates)
             } catch (e: Exception) {}
         }
    }

    fun loadVerificationStatus() {
        viewModelScope.launch {
            try {
                _verificationStatus.value = repository.apiService.getVerificationStatus()
            } catch (e: Exception) {}
        }
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.apiService.getMyProfile()
            } catch (e: Exception) {
               // Handle error
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.apiService.getUserProfile(userId)
            } catch (e: Exception) {
               // Handle error
            }
        }
    }

    fun requestVerification(birthDate: String) {
        viewModelScope.launch {
            try {
                val result = repository.apiService.requestVerification(com.example.data.models.VerificationCriteria(birthDate))
                if (result.verified) {
                    loadMyProfile()
                }
            } catch (e: Exception) {
                // error handling
            }
        }
    }
}
