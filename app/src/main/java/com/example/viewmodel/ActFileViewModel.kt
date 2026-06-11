package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ActFile
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActFileViewModel(private val repository: CmoRepository) : ViewModel() {
    private val _actFiles = MutableStateFlow<List<ActFile>>(emptyList())
    val actFiles: StateFlow<List<ActFile>> = _actFiles.asStateFlow()

    private val _replies = MutableStateFlow<Map<String, List<com.example.data.models.ActFileReply>>>(emptyMap())
    val replies = _replies.asStateFlow()

    init {
        loadActFiles()
    }

    fun loadReplies(id: String) {
        viewModelScope.launch {
            try {
                _replies.value = _replies.value + (id to repository.apiService.getActFileReplies(id))
            } catch (e: Exception) {}
        }
    }

    fun addReply(id: String, content: String) {
        viewModelScope.launch {
            try {
                val newReply = repository.apiService.addActFileReply(id, mapOf("content" to content))
                val current = _replies.value[id] ?: emptyList()
                _replies.value = _replies.value + (id to listOf(newReply) + current)
                
                _actFiles.value = _actFiles.value.map {
                    if (it.id == id) it.copy(repliesCount = it.repliesCount + 1) else it
                }
            } catch (e: Exception) {}
        }
    }

    fun loadActFiles() {
        viewModelScope.launch {
            try {
                _actFiles.value = repository.apiService.getActFiles()
            } catch (e: Exception) {
               // Handle
            }
        }
    }

    fun createActFile(content: String) {
        viewModelScope.launch {
            try {
                val newFile = repository.apiService.createActFile(mapOf("content" to content))
                _actFiles.value = listOf(newFile) + _actFiles.value
            } catch (e: Exception) {
               // Handle
            }
        }
    }

    fun likeActFile(id: String) {
        viewModelScope.launch {
            try {
                val resp = repository.apiService.likeActFile(id)
                val isLiked = resp["liked"] == true
                _actFiles.value = _actFiles.value.map {
                    if (it.id == id) {
                        it.copy(
                            liked = isLiked,
                            likesCount = if (isLiked) it.likesCount + 1 else it.likesCount - 1
                        )
                    } else it
                }
            } catch (e: Exception) {}
        }
    }
}
