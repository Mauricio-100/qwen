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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.MultipartBody
import com.example.data.models.toVideo

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

    private val _searchMetadata = MutableStateFlow<com.example.data.models.SearchMetadata?>(null)
    val searchMetadata = _searchMetadata.asStateFlow()

    private val _verificationStatus = MutableStateFlow<com.example.data.models.VerificationStatusResponse?>(null)
    val verificationStatus: StateFlow<com.example.data.models.VerificationStatusResponse?> = _verificationStatus.asStateFlow()

    val isDarkThemeFlow = repository.prefs.isDarkThemeFlow
    val downloadedVideos = repository.downloadManager?.getDownloadedVideos() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val isOfflineFlow = repository.isOfflineFlow
    val offlineSimFlow = repository.prefs.offlineSimFlow
    val downloadQualityFlow = repository.prefs.downloadQualityFlow

    fun setOfflineSimMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.prefs.setOfflineSimMode(enabled)
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            repository.prefs.setDownloadQuality(quality)
        }
    }

    fun clearCacheAndDownloads(context: android.content.Context, callback: () -> Unit) {
        viewModelScope.launch {
            repository.clearCache()
            try {
                val dir = java.io.File(context.filesDir, "downloads")
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            callback()
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
        // Obsolete: there is no GET /api/users endpoint.
        _allUsers.value = emptyList()
    }

    fun searchUsersByName(query: String) {
        if (query.length < 2) {
            _allUsers.value = emptyList()
            _searchMetadata.value = null
            return
        }
        viewModelScope.launch {
            try {
                val response = repository.apiService.searchUsers(query, type = "users")
                _allUsers.value = response.results
                _searchMetadata.value = response.metadata
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error searching users: ${e.message}")
            }
        }
    }

    fun searchVideosByQuery(
        query: String,
        date: String? = null,
        views: String? = null,
        duration: String? = null,
        tag: String? = null,
        mention: String? = null,
        relevance: String? = null
    ) {
        if (query.length < 2) {
            _searchedVideos.value = emptyList()
            _searchMetadata.value = null
            return
        }
        viewModelScope.launch {
            try {
                val response = repository.apiService.searchVideos(
                    query = query,
                    type = "videos",
                    date = date,
                    views = views,
                    duration = duration,
                    tag = tag,
                    mention = mention,
                    relevance = relevance
                )
                _searchMetadata.value = response.metadata
                val videos = response.results
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
                if (me.isNotEmpty()) {
                    val profile: com.example.data.models.User = _userProfile.value ?: repository.apiService.getUserProfile(me)
                    val response = repository.apiService.getUserWings(me)
                    val mapped = response.wings.map { 
                        it.toVideo(
                            username = profile.username,
                            avatarUrl = profile.avatarUrl,
                            isVerified = profile.isVerified
                        )
                    }
                    _myVideos.value = mapped
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading my videos: ${e.message}")
            }
        }
    }

    fun loadUserVideos(userId: String) {
        viewModelScope.launch {
            try {
                val profile: com.example.data.models.User = _userProfile.value ?: repository.apiService.getUserProfile(userId)
                val response = repository.apiService.getUserWings(userId)
                val mapped = response.wings.map { 
                    it.toVideo(
                        username = profile.username,
                        avatarUrl = profile.avatarUrl,
                        isVerified = profile.isVerified
                    )
                }
                _myVideos.value = mapped
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
                val profile: com.example.data.models.User = _userProfile.value ?: repository.apiService.getUserProfile(userId)
                val soundsList = repository.apiService.getUserSounds(userId)
                val mapped = soundsList.map {
                    it.toVideo(
                        username = profile.username,
                        avatarUrl = profile.avatarUrl,
                        isVerified = profile.isVerified
                    )
                }
                _userSounds.value = mapped
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
             } catch (e: Exception) {
                 Log.e("ProfileViewModel", "JSON updateProfile failed, trying multipart: ${e.message}")
                 try {
                     val bioBody = updates["bio"]?.let { okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), it) }
                     val phoneBody = updates["phone_number"]?.let { okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), it) }
                     _userProfile.value = repository.apiService.updateProfileMultipart(
                         bio = bioBody,
                         phoneNumber = phoneBody
                     )
                 } catch (ex: Exception) {
                     Log.e("ProfileViewModel", "Both updates failed: ${ex.message}")
                 }
             }
         }
    }

    fun updateAvatar(context: android.content.Context, uri: android.net.Uri) {
         viewModelScope.launch {
             try {
                 val contentResolver = context.contentResolver
                 val inputStream = contentResolver.openInputStream(uri)
                 val bytes = inputStream?.readBytes() ?: return@launch
                 
                 val requestFile = okhttp3.RequestBody.create(
                     (contentResolver.getType(uri) ?: "image/*").toMediaTypeOrNull(),
                     bytes
                 )
                 val body = okhttp3.MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestFile)
                 
                 try {
                     val user = repository.apiService.updateAvatar(body)
                     _userProfile.value = user
                 } catch (e: Exception) {
                     Log.w("ProfileViewModel", "updateAvatar failed, falling back to updateProfileMultipart: ${e.message}")
                     val user = repository.apiService.updateProfileMultipart(
                         avatar = body
                     )
                     _userProfile.value = user
                 }
              } catch (e: Exception) {
                 Log.e("ProfileViewModel", "Error uploading avatar: ${e.message}")
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
                repository.getMyProfile().onSuccess {
                    _userProfile.value = it
                }
            } catch (e: Exception) {
               // Handle error
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                repository.getUserProfile(userId).onSuccess {
                    _userProfile.value = it
                }
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

    fun toggleFollowInList(targetUser: User) {
        viewModelScope.launch {
            try {
                val resp = repository.apiService.followUser(targetUser.id)
                val isNowFollowing = resp["following"] as? Boolean ?: !targetUser.isFollowing
                
                _allUsers.value = _allUsers.value.map {
                    if (it.id == targetUser.id) {
                        it.copy(
                            isFollowing = isNowFollowing,
                            followersCount = if (isNowFollowing) it.followersCount + 1 else (it.followersCount - 1).coerceAtLeast(0)
                        )
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error toggling follow search flow: ${e.message}")
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
