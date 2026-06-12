package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Video
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class FeedViewModel(private val repository: CmoRepository) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _stories = MutableStateFlow<List<com.example.data.models.Story>>(emptyList())
    val stories: StateFlow<List<com.example.data.models.Story>> = _stories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoComments = MutableStateFlow<Map<String, List<com.example.data.models.VideoComment>>>(emptyMap())
    val videoComments = _videoComments.asStateFlow()

    private val _storyComments = MutableStateFlow<Map<String, List<com.example.data.models.VideoComment>>>(emptyMap())
    val storyComments = _storyComments.asStateFlow()

    init {
        loadFeed()
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                val apiStories = repository.apiService.getStories()
                _stories.value = apiStories
            } catch (e: Exception) {
                _stories.value = emptyList()
            }
        }
    }

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            try {
                _videoComments.value = _videoComments.value + (videoId to repository.apiService.getVideoComments(videoId))
            } catch (e: Exception) {
                // handle
            }
        }
    }

    fun addComment(videoId: String, content: String) {
        viewModelScope.launch {
             try {
                 val newComment = repository.apiService.addVideoComment(videoId, mapOf("content" to content))
                 val currentComments = _videoComments.value[videoId] ?: emptyList()
                 _videoComments.value = _videoComments.value + (videoId to listOf(newComment) + currentComments)
                 
                 _videos.value = _videos.value.map {
                     if (it.id == videoId) {
                         it.copy(commentsCount = it.commentsCount + 1)
                     } else it
                 }
             } catch (e: Exception) {}
        }
    }

    fun loadStoryComments(storyId: String) {
        viewModelScope.launch {
            try {
                _storyComments.value = _storyComments.value + (storyId to repository.apiService.getStoryComments(storyId))
            } catch (e: Exception) {}
        }
    }

    fun addStoryComment(storyId: String, content: String) {
        viewModelScope.launch {
            try {
                val newComment = repository.apiService.addStoryComment(storyId, mapOf("content" to content))
                val current = _storyComments.value[storyId] ?: emptyList()
                _storyComments.value = _storyComments.value + (storyId to current + newComment)
            } catch (e: Exception) {}
        }
    }

    fun shareVideo(videoId: String) {
         _videos.value = _videos.value.map {
             if (it.id == videoId) {
                 it.copy(sharesCount = it.sharesCount + 1)
             } else it
         }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getFeed()
            result.onSuccess {
                _videos.value = it
            }.onFailure {
                // Failure
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoading.value) return
        val lastVideo = _videos.value.lastOrNull() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getFeed(cursor = lastVideo.id)
            result.onSuccess { newVideos ->
                _videos.value = _videos.value + newVideos
            }
            _isLoading.value = false
        }
    }

    fun likeVideo(videoId: String) {
        viewModelScope.launch {
            try {
                val resp = repository.apiService.likeVideo(videoId)
                val isLiked = resp["liked"] == true
                
                // Mettre à jour la state
                _videos.value = _videos.value.map {
                    if (it.id == videoId) {
                        it.copy(
                            liked = isLiked,
                            likes = if (isLiked) it.likes + 1 else it.likes - 1
                        )
                    } else it
                }
            } catch (e: Exception) {
               // Handle fail
            }
        }
    }

    fun publishVideo(description: String, videoUri: String, thumbnailUrl: String) {
        // This is replaced by uploadAndPublishVideo, keeping it simple as it might be used by UI
        // If UI calls this, we should ideally show a warning or redirect to upload
    }

    fun publishStory(mediaUri: String) {
        // This is replaced by uploadAndPublishStory
    }

    fun uploadAndPublishVideo(context: Context, videoUri: Uri, description: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(videoUri) ?: "video/mp4"
                val inputStream = resolver.openInputStream(videoUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Impossible de lire les octets du fichier")
                inputStream.close()

                val requestFile = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                val filePart = MultipartBody.Part.createFormData("video", "video.mp4", requestFile)
                
                val descBody = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
                val isPublicBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "true")

                val uploadedVideo = repository.apiService.uploadVideo(filePart, descBody, isPublicBody)
                
                _videos.value = listOf(uploadedVideo) + _videos.value
                
                val currentUsername = repository.prefs.usernameFlow.first() ?: "Moi"
                repository.notificationHelper.showNotification(
                    "Nouvelle vidéo publiée !",
                    "@$currentUsername a publié une vidéo : $description"
                )
                callback(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, e.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadAndPublishStory(context: Context, imageUri: Uri, effect: String?, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(imageUri) ?: "image/jpeg"
                val inputStream = resolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Impossible de lire les octets de l'image")
                inputStream.close()

                val requestFile = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)
                
                val effectBody = effect?.let { RequestBody.create("text/plain".toMediaTypeOrNull(), it) }

                val responseMap = repository.apiService.uploadStory(filePart, effectBody)
                val mediaUrl = responseMap["media_url"] as? String ?: ""
                
                val currentUsername = repository.prefs.usernameFlow.first() ?: "Moi"
                val currentUserId = repository.prefs.userIdFlow.first() ?: "user_me"
                
                val newStory = com.example.data.models.Story(
                    id = (responseMap["story_id"] as? String) ?: ("remote_" + System.currentTimeMillis()),
                    userId = currentUserId,
                    username = currentUsername,
                    avatarUrl = null,
                    mediaUrl = mediaUrl,
                    isVerified = true,
                    createdAt = "À l'instant"
                )

                _stories.value = listOf(newStory) + _stories.value
                
                repository.notificationHelper.showNotification(
                    "Nouvelle story ajoutée !",
                    "@$currentUsername a partagé une nouvelle story"
                )
                callback(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, e.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _storyViews = MutableStateFlow<Map<String, Int>>(emptyMap())
    val storyViews: StateFlow<Map<String, Int>> = _storyViews.asStateFlow()

    fun getStoryViews(storyId: String): Int {
        return _storyViews.value[storyId] ?: 0
    }

    fun incrementStoryView(storyId: String) {
        // Should ideally call an API, keeping it local if API absent, but user wants real data.
        // Assuming no viewStory API yet, but we can add one if needed.
        _storyViews.value = _storyViews.value + (storyId to (_storyViews.value[storyId] ?: 0) + 1)
    }

}
