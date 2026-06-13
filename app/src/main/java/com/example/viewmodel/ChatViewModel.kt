package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Conversation
import com.example.data.models.Message
import com.example.data.models.User
import com.example.data.repository.StripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: StripRepository) : ViewModel() {

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
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _selectedWallpaper = MutableStateFlow("default")
    val selectedWallpaper: StateFlow<String> = _selectedWallpaper.asStateFlow()

    private val _activeChatUserId = MutableStateFlow<String?>(null)
    val activeChatUserId: StateFlow<String?> = _activeChatUserId.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prefs.usernameFlow.collect {
                _currentUsername.value = it ?: ""
            }
        }
        viewModelScope.launch {
            repository.prefs.userIdFlow.collect {
                _currentUserId.value = it ?: ""
            }
        }
        loadStories()
        loadContacts()

        // Ecouter les messages websockets en temps réel
        repository.wsManager.messageEvents.onEach { json ->
            val type = json.optString("type")
            if (type == "new_message") {
                val senderUsername = json.optString("sender_username")
                val content = json.optString("content")
                val senderId = json.optString("sender_id")
                val msgId = json.optString("message_id")
                
                val newMessage = Message(
                    id = msgId,
                    content = content,
                    type = "text",
                    senderId = senderId,
                    receiverId = "", // Current user
                    read = false,
                    senderUsername = senderUsername,
                    senderAvatar = null
                )
                
                val activeId = _activeChatUserId.value
                val myId = _currentUserId.value
                
                // Mettre à jour l'écran de chat actif si le message provient de notre interlocuteur actuel
                if (activeId != null && senderId == activeId) {
                    val alreadyInList = _messages.value.any { it.id == msgId || (it.content == content && it.senderUsername == senderUsername && (System.currentTimeMillis() - (it.id.substringAfter("local_").toLongOrNull() ?: 0L)) < 15000) }
                    if (!alreadyInList) {
                        _messages.value = _messages.value + newMessage
                    }
                }
                
                loadConversations() // update last message in list
                
                // Ne pas notifier notre propre message envoyé depuis un autre appareil
                if (senderId != myId && senderId != "me") {
                    repository.notificationHelper.showNotification(senderUsername, content)
                }
            } else if (type == "message_sent") {
                val msgId = json.optString("message_id")
                // Remplacement de l'ID temporaire par le vrai ID de la base de données
                _messages.value = _messages.value.map { msg ->
                    if (msg.id.startsWith("local_")) {
                        msg.copy(id = msgId)
                    } else {
                        msg
                    }
                }
                loadConversations()
            } else if (type == "typing") {
                val senderId = json.optString("sender_id")
                val isTyping = json.optBoolean("is_typing")
                if (senderId == _activeChatUserId.value) {
                    _isTyping.value = isTyping
                }
            }
        }.launchIn(viewModelScope)
    }

    fun setActiveChat(userId: String?) {
        _activeChatUserId.value = userId
        if (userId != null) {
            loadMessages(userId)
        } else {
            _messages.value = emptyList()
        }
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
        val tempId = "local_${System.currentTimeMillis()}"
        val tempMsg = Message(
            id = tempId,
            content = content,
            type = "text",
            senderId = "me",
            receiverId = receiverId,
            read = true,
            senderUsername = username,
            senderAvatar = null,
            isVerified = true
        )
        // Affichage instantané du message pour l'utilisateur (zéro latence)
        _messages.value = _messages.value + tempMsg
        
        var sentViaWs = false
        // Envoi instantané via WebSocket
        try {
            sentViaWs = repository.wsManager.sendMessage(receiverId, content, username)
        } catch (e: Exception) {
            android.util.Log.e("ChatVM", "Failed to send WebSocket message", e)
        }

        // Si l'envoi via WebSocket a échoué (ex: hors ligne), on utilise l'API REST en tâche de fond comme fallback
        if (!sentViaWs) {
            viewModelScope.launch {
                try {
                    val resp = repository.apiService.sendMessage(mapOf(
                        "receiver_id" to receiverId,
                        "content" to content,
                        "type" to "text"
                    ))
                    // Remplacement du message temporaire par le message officiel
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == tempId) resp else msg
                    }
                    loadConversations()
                } catch (e: Exception) {
                    android.util.Log.e("ChatVM", "Background API fallback failed", e)
                }
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
