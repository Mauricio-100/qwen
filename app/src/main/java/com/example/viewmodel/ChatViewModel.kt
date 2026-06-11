package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Conversation
import com.example.data.models.Message
import com.example.data.repository.CmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: CmoRepository) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _stories = MutableStateFlow<List<com.example.data.models.Story>>(emptyList())
    val stories: StateFlow<List<com.example.data.models.Story>> = _stories.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentUsername = MutableStateFlow("")
    val currentUsername: StateFlow<String> = _currentUsername.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prefs.usernameFlow.collect {
                _currentUsername.value = it ?: ""
            }
        }
        loadStories()

        // Ecouter les messages websockets
        repository.wsManager.messageEvents.onEach { json ->
            if (json.optString("type") == "new_message") {
                val senderUsername = json.optString("sender_username")
                val content = json.optString("content")
                val newMessage = Message(
                    id = json.optString("message_id"),
                    content = content,
                    type = "text",
                    senderId = json.optString("sender_id"),
                    receiverId = "", // Current user
                    read = false,
                    senderUsername = senderUsername,
                    senderAvatar = null
                )
                _messages.value = _messages.value + newMessage
                loadConversations() // update last message
                repository.notificationHelper.showNotification(senderUsername, content)
            }
        }.launchIn(viewModelScope)
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _conversations.value = repository.apiService.getConversations()
            } catch (e: Exception) {
               // Handle error
            }
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                _stories.value = repository.apiService.getStories()
            } catch (e: Exception) {}
        }
    }

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            try {
                _messages.value = repository.apiService.getMessages(userId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendMessage(receiverId: String, content: String, username: String) {
        viewModelScope.launch {
            try {
                val resp = repository.apiService.sendMessage(mapOf(
                    "receiver_id" to receiverId,
                    "content" to content,
                    "type" to "text"
                ))
                _messages.value = _messages.value + resp
                repository.wsManager.sendMessage(receiverId, content, username)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
