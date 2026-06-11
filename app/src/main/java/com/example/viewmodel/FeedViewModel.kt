package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Video
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: CmoRepository) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _stories = MutableStateFlow<List<com.example.data.models.Story>>(emptyList())
    val stories: StateFlow<List<com.example.data.models.Story>> = _stories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoComments = MutableStateFlow<Map<String, List<com.example.data.models.VideoComment>>>(emptyMap())
    val videoComments = _videoComments.asStateFlow()

    init {
        loadFeed()
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                _stories.value = repository.apiService.getStories()
            } catch (e: Exception) {
                // handle error
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
                // Pourrait utiliser les cachedVideos ici
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
}
