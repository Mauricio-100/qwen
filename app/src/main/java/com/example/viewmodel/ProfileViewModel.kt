package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.User
import com.example.data.models.Video
import com.example.data.repository.StripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import android.util.Log

class ProfileViewModel(private val repository: StripRepository) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _myVideos = MutableStateFlow<List<Video>>(emptyList())
    val myVideos: StateFlow<List<Video>> = _myVideos.asStateFlow()

    private val _serverStats = MutableStateFlow<Map<String, Any>?>(null)
    val serverStats = _serverStats.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _searchedVideos = MutableStateFlow<List<Video>>(emptyList())
    val searchedVideos = _searchedVideos.asStateFlow()

    private val _verificationStatus = MutableStateFlow<com.example.data.models.VerificationStatusResponse?>(null)
    val verificationStatus: StateFlow<com.example.data.models.VerificationStatusResponse?> = _verificationStatus.asStateFlow()

    val isDarkThemeFlow = repository.prefs.isDarkThemeFlow
    val downloadedVideos = repository.downloadManager?.getDownloadedVideos() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun logout() {
        viewModelScope.launch {
            repository.prefs.clearAuth()
            repository.disconnectWebSocket()
        }
    }

    fun setThemeMode(isDark: Boolean) {
        viewModelScope.launch {
            repository.prefs.setThemeMode(isDark)
        }
    }

    private val _notifications = MutableStateFlow<List<com.example.data.models.Notification>>(emptyList())
    val notifications: StateFlow<List<com.example.data.models.Notification>> = _notifications.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val result = repository.getNotifications()
                result.onSuccess {
                    _notifications.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAllUsers() {
        // Obsolete: there is no GET /api/users endpoint.
        _allUsers.value = emptyList()
    }

    fun searchUsersByName(query: String) {
        if (query.length < 2) {
            _allUsers.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _allUsers.value = repository.apiService.searchUsers(query, type = "users")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error searching users: ${e.message}")
            }
        }
    }

    fun searchVideosByQuery(query: String) {
        if (query.length < 2) {
            _searchedVideos.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val videos = repository.apiService.searchVideos(query, type = "videos")
                // Reconstruct full URLs from thumbnails since backend search might omit video_url
                _searchedVideos.value = videos.map { video ->
                    if (video.videoUrl.isBlank() && video.thumbnailUrl.isNotBlank()) {
                        video.copy(videoUrl = video.thumbnailUrl.replace(".jpg", ".mp4"))
                    } else {
                        video
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error searching videos: ${e.message}")
            }
        }
    }

    fun loadServerStats() {
        viewModelScope.launch {
            try {
                _serverStats.value = repository.apiService.getServerStats()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading server stats: ${e.message}")
            }
        }
    }

    fun loadMyVideos() {
        viewModelScope.launch {
            try {
                val me = repository.prefs.userIdFlow.firstOrNull() ?: ""
                val meUsername = repository.prefs.usernameFlow.firstOrNull() ?: ""
                if (me.isNotEmpty()) {
                    val fetched = try {
                        repository.apiService.getUserVideos(me)
                    } catch (e: Exception) {
                        Log.w("ProfileViewModel", "getUserVideos endpoint failed (likely 404). Falling back to feed and search query...")
                        val feedVideos = try { repository.apiService.getFeed(limit = 100) } catch (ex: Exception) { emptyList() }
                        val feedUserVideos = feedVideos.filter { it.userId == me }
                        
                        val searchVideos = if (meUsername.isNotEmpty()) {
                            try { repository.apiService.searchVideos(meUsername) } catch (ex: Exception) { emptyList() }
                        } else emptyList()
                        val searchUserVideos = searchVideos.filter { it.userId == me }
                        
                        (feedUserVideos + searchUserVideos).distinctBy { it.id }
                    }
                    _myVideos.value = fetched
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading my videos: ${e.message}")
            }
        }
    }

    fun loadUserVideos(userId: String) {
        viewModelScope.launch {
            try {
                val fetched = try {
                    repository.apiService.getUserVideos(userId)
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "getUserVideos failed for target user. Falling back to feed index...")
                    val feedVideos = try { repository.apiService.getFeed(limit = 100) } catch (ex: Exception) { emptyList() }
                    feedVideos.filter { it.userId == userId }
                }
                _myVideos.value = fetched
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user videos: ${e.message}")
            }
        }
    }

    private val _userSounds = MutableStateFlow<List<Video>>(emptyList())
    val userSounds: StateFlow<List<Video>> = _userSounds.asStateFlow()

    fun loadUserSounds(userId: String) {
        viewModelScope.launch {
            try {
                _userSounds.value = repository.apiService.getUserSounds(userId)
            } catch (e: Exception) {}
        }
    }

    fun loadMySavedSounds() {
        viewModelScope.launch {
            try {
                _userSounds.value = repository.apiService.getSavedSounds()
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

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                repository.apiService.followUser(userId)
                // Refresh profile to see updated follower count
                loadUserProfile(userId)
            } catch (e: Exception) {}
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
