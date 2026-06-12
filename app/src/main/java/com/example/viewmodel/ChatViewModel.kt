package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Conversation
import com.example.data.models.Message
import com.example.data.models.User
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

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    private val _currentUsername = MutableStateFlow("")
    val currentUsername: StateFlow<String> = _currentUsername.asStateFlow()

    private val _messageReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val messageReactions: StateFlow<Map<String, String>> = _messageReactions.asStateFlow()

    private val _selectedWallpaper = MutableStateFlow("default")
    val selectedWallpaper: StateFlow<String> = _selectedWallpaper.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prefs.usernameFlow.collect {
                _currentUsername.value = it ?: ""
            }
        }
        loadStories()
        loadContacts()

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
                val list = repository.apiService.getConversations()
                _conversations.value = list
            } catch (e: Exception) {
                _conversations.value = emptyList()
            }
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                val s = repository.apiService.getStories()
                _stories.value = s
            } catch (e: Exception) {
                _stories.value = emptyList()
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                val u = repository.apiService.getUsers()
                _contacts.value = u
            } catch (e: Exception) {
                _contacts.value = emptyList()
            }
        }
    }

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            try {
                val list = repository.apiService.getMessages(userId)
                _messages.value = list
            } catch (e: Exception) {
                _messages.value = emptyList()
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
                // Fallback to local messaging to prevent any crash in offline modes
                val localMsg = Message(
                    id = "local_${System.currentTimeMillis()}",
                    content = content,
                    type = "text",
                    senderId = "me",
                    receiverId = receiverId,
                    read = true,
                    senderUsername = username,
                    senderAvatar = null,
                    isVerified = true
                )
                _messages.value = _messages.value + localMsg
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val current = _messageReactions.value.toMutableMap()
        if (current[messageId] == emoji) {
            current.remove(messageId) // toggle off
        } else {
            current[messageId] = emoji
        }
        _messageReactions.value = current
    }

    fun changeWallpaper(wallpaper: String) {
        _selectedWallpaper.value = wallpaper
    }


}
