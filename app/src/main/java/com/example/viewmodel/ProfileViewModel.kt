package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.User
import com.example.data.models.Video
import com.example.data.repository.StripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ProfileViewModel(private val repository: StripRepository) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _myVideos = MutableStateFlow<List<Video>>(emptyList())
    val myVideos: StateFlow<List<Video>> = _myVideos.asStateFlow()

    private val _serverStats = MutableStateFlow<Map<String, Any>?>(null)
    val serverStats = _serverStats.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    private val _followInFlightIds = MutableStateFlow<Set<String>>(emptySet())
    val followInFlightIds: StateFlow<Set<String>> = _followInFlightIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _verificationStatus = MutableStateFlow<com.example.data.models.VerificationStatusResponse?>(null)
    val verificationStatus: StateFlow<com.example.data.models.VerificationStatusResponse?> = _verificationStatus.asStateFlow()

    val isDarkThemeFlow = repository.prefs.isDarkThemeFlow
    val downloadedVideos = repository.downloadManager?.getDownloadedVideos() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    init {
        viewModelScope.launch {
            repository.prefs.userIdFlow.collect { userId ->
                _currentUserId.value = userId
            }
        }
    }

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
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                val me = _currentUserId.value ?: repository.prefs.userIdFlow.firstOrNull()
                _allUsers.value = repository.apiService
                    .getUsers()
                    .filterNot { it.id == me }
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de charger les profils."
            } finally {
                _isLoadingUsers.value = false
            }
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

    fun loadUserVideos(userId: String) {
        viewModelScope.launch {
            try {
                _myVideos.value = repository.apiService.getUserVideos(userId)
            } catch (e: Exception) {}
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
                 val updatedProfile = repository.apiService.updateProfile(updates)
                 _userProfile.value = updatedProfile
                 syncUserCaches(updatedProfile)
             } catch (e: Exception) {
                 _errorMessage.value = "Impossible de mettre le profil a jour."
             }
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
                val profile = repository.apiService.getMyProfile()
                _userProfile.value = profile
                syncUserCaches(profile)
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de charger votre profil."
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = repository.apiService.getUserProfile(userId)
                _userProfile.value = profile
                syncUserCaches(profile)
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de charger ce profil."
            }
        }
    }

    fun followUser(userId: String) {
        if (_followInFlightIds.value.contains(userId)) return
        viewModelScope.launch {
            _followInFlightIds.update { it + userId }
            try {
                val previousUser = _allUsers.value.firstOrNull { it.id == userId }
                    ?: _userProfile.value?.takeIf { it.id == userId }
                val previousIsFollowing = previousUser?.isFollowing ?: false
                val response = repository.apiService.followUser(userId)
                val isFollowing = response["is_following"] as? Boolean ?: !previousIsFollowing

                _allUsers.value = _allUsers.value.map { user ->
                    if (user.id == userId) {
                        user.copy(
                            isFollowing = isFollowing,
                            followersCount = updateCount(user.followersCount, user.isFollowing, isFollowing)
                        )
                    } else {
                        user
                    }
                }

                _userProfile.value = _userProfile.value?.let { profile ->
                    when (profile.id) {
                        userId -> profile.copy(
                            isFollowing = isFollowing,
                            followersCount = updateCount(profile.followersCount, profile.isFollowing, isFollowing)
                        )

                        _currentUserId.value -> profile.copy(
                            followingCount = updateCount(profile.followingCount, previousIsFollowing, isFollowing)
                        )

                        else -> profile
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de mettre a jour l'abonnement."
            } finally {
                _followInFlightIds.update { it - userId }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
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

    private fun syncUserCaches(updatedUser: User) {
        _allUsers.value = _allUsers.value.map { user ->
            if (user.id == updatedUser.id) updatedUser else user
        }
    }

    private fun updateCount(current: Int, previousValue: Boolean, nextValue: Boolean): Int {
        return when {
            previousValue == nextValue -> current
            !previousValue && nextValue -> current + 1
            previousValue && !nextValue -> maxOf(0, current - 1)
            else -> current
        }
    }
}
