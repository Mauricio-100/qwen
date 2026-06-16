package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPlaylist
import com.example.data.local.LocalPlaylistVideo
import com.example.data.models.Playlist
import com.example.data.models.Video
import com.example.data.repository.StripRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PlaylistViewModel(private val repository: StripRepository) : ViewModel() {

    private val db = repository.db
    private val api = repository.apiService

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _playlistVideos = MutableStateFlow<List<Video>>(emptyList())
    val playlistVideos: StateFlow<List<Video>> = _playlistVideos.asStateFlow()

    private val _activePlaylist = MutableStateFlow<Playlist?>(null)
    val activePlaylist: StateFlow<Playlist?> = _activePlaylist.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        // Observe local playlists and auto-map them to our domain Playlist models
        viewModelScope.launch {
            repository.prefs.userIdFlow.collect { uid ->
                val userId = uid ?: ""
                if (userId.isNotEmpty()) {
                    db.playlistDao().getPlaylistsFlow(userId).collect { localList ->
                        // Automatically update _playlists when local changes occur (Offline-First!)
                        val domainPlaylists = localList.map { local ->
                            val count = db.playlistDao().getVideosCount(local.id)
                            Playlist(
                                id = local.id,
                                name = local.name,
                                description = local.description,
                                userId = local.userId,
                                createdAt = local.createdAt.toString(),
                                videosCount = count
                            )
                        }
                        _playlists.value = domainPlaylists
                    }
                }
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = repository.prefs.userIdFlow.first() ?: ""
                val isOffline = repository.isOfflineFlow.first()

                if (isOffline) {
                    // Load purely from local db (handled already reactive, but let's refresh)
                    val localList = db.playlistDao().getPlaylists(userId)
                    val domainPlaylists = localList.map { local ->
                        val count = db.playlistDao().getVideosCount(local.id)
                        Playlist(
                            id = local.id,
                            name = local.name,
                            description = local.description,
                            userId = local.userId,
                            createdAt = local.createdAt.toString(),
                            videosCount = count
                        )
                    }
                    _playlists.value = domainPlaylists
                } else {
                    // Online: Get from server and mirror/sync with Room
                    val response = api.getPlaylists()
                    // Save to local database
                    for (pl in response) {
                        db.playlistDao().insertPlaylist(
                            LocalPlaylist(
                                id = pl.id,
                                name = pl.name,
                                description = pl.description,
                                userId = pl.userId,
                                createdAt = pl.createdAt.toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                    _playlists.value = response
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading playlists: ${e.message}")
                _message.emit("Erreur lors du chargement des playlists")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String, description: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = repository.prefs.userIdFlow.first() ?: ""
                val isOffline = repository.isOfflineFlow.first()

                val newId = UUID.randomUUID().toString()
                val playlistModel = Playlist(
                    id = newId,
                    name = name,
                    description = description,
                    userId = userId,
                    createdAt = System.currentTimeMillis().toString(),
                    videosCount = 0
                )

                if (isOffline) {
                    // Offline creation
                    db.playlistDao().insertPlaylist(
                        LocalPlaylist(
                            id = newId,
                            name = name,
                            description = description,
                            userId = userId
                        )
                    )
                    _playlists.value = listOf(playlistModel) + _playlists.value
                    _message.emit("Playlist créée hors-ligne 🐆")
                    onSuccess()
                } else {
                    // Online server call
                    val requestBody = mapOf("name" to name, "description" to description)
                    val response = api.createPlaylist(requestBody)
                    // Cache locally
                    db.playlistDao().insertPlaylist(
                        LocalPlaylist(
                            id = response.id,
                            name = response.name,
                            description = response.description,
                            userId = response.userId,
                            createdAt = response.createdAt.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error creating playlist: ${e.message}")
                _message.emit("Erreur de création de playlist")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editPlaylist(playlistId: String, name: String, description: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = repository.prefs.userIdFlow.first() ?: ""
                val isOffline = repository.isOfflineFlow.first()

                if (isOffline) {
                    val local = db.playlistDao().getPlaylistById(playlistId)
                    if (local != null) {
                        db.playlistDao().insertPlaylist(
                            local.copy(name = name, description = description)
                        )
                        _message.emit("Playlist modifiée hors-ligne")
                        onSuccess()
                    }
                } else {
                    val request = mapOf("name" to name, "description" to description)
                    val response = api.editPlaylist(playlistId, request)
                    db.playlistDao().insertPlaylist(
                        LocalPlaylist(
                            id = response.id,
                            name = response.name,
                            description = response.description,
                            userId = response.userId,
                            createdAt = response.createdAt.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error editing playlist: ${e.message}")
                _message.emit("Erreur lors de la modification")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlaylist(playlistId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isOffline = repository.isOfflineFlow.first()

                if (isOffline) {
                    db.playlistDao().deletePlaylist(playlistId)
                    _message.emit("Playlist supprimée hors-ligne")
                    onSuccess()
                } else {
                    api.deletePlaylist(playlistId)
                    db.playlistDao().deletePlaylist(playlistId)
                    _message.emit("Playlist supprimée")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error deleting playlist: ${e.message}")
                _message.emit("Erreur lors de la suppression")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPlaylistVideos(playlistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isOffline = repository.isOfflineFlow.first()
                if (isOffline) {
                    val localVideos = db.playlistDao().getPlaylistVideos(playlistId)
                    _playlistVideos.value = localVideos.map { local ->
                        Video(
                            id = local.videoId,
                            videoUrl = local.videoUrl,
                            thumbnailUrl = local.thumbnailUrl,
                            description = local.description,
                            username = local.username,
                            likes = local.likes,
                            views = local.views
                        )
                    }
                } else {
                    val response = api.getPlaylistVideos(playlistId)
                    // Sync locally
                    for (v in response) {
                        db.playlistDao().insertPlaylistVideo(
                            LocalPlaylistVideo(
                                playlistId = playlistId,
                                videoId = v.id,
                                videoUrl = v.videoUrl,
                                thumbnailUrl = v.thumbnailUrl,
                                description = v.description,
                                username = v.username,
                                likes = v.likes,
                                views = v.views
                            )
                        )
                    }
                    _playlistVideos.value = response
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading videos: ${e.message}")
                _message.emit("Erreur de chargement des vidéos de playlist")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addVideoToPlaylist(playlist: Playlist, video: Video, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val isOffline = repository.isOfflineFlow.first()
                if (isOffline) {
                    db.playlistDao().insertPlaylistVideo(
                        LocalPlaylistVideo(
                            playlistId = playlist.id,
                            videoId = video.id,
                            videoUrl = video.videoUrl,
                            thumbnailUrl = video.thumbnailUrl,
                            description = video.description,
                            username = video.username,
                            likes = video.likes,
                            views = video.views
                        )
                    )
                    _message.emit("Vidéo ajoutée à ${playlist.name} (Hors-ligne) 🐆")
                    onComplete()
                } else {
                    api.addVideoToPlaylist(playlist.id, video.id)
                    db.playlistDao().insertPlaylistVideo(
                        LocalPlaylistVideo(
                            playlistId = playlist.id,
                            videoId = video.id,
                            videoUrl = video.videoUrl,
                            thumbnailUrl = video.thumbnailUrl,
                            description = video.description,
                            username = video.username,
                            likes = video.likes,
                            views = video.views
                        )
                    )
                    _message.emit("Vidéo ajoutée à ${playlist.name} 🐆")
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error adding video: ${e.message}")
                _message.emit("Erreur lors de l'ajout à la playlist")
            }
        }
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            try {
                val isOffline = repository.isOfflineFlow.first()
                if (isOffline) {
                    db.playlistDao().deletePlaylistVideo(playlistId, videoId)
                    _playlistVideos.value = _playlistVideos.value.filter { it.id != videoId }
                    _message.emit("Vidéo retirée hors-ligne")
                } else {
                    api.removeVideoFromPlaylist(playlistId, videoId)
                    db.playlistDao().deletePlaylistVideo(playlistId, videoId)
                    _playlistVideos.value = _playlistVideos.value.filter { it.id != videoId }
                    _message.emit("Vidéo retirée de la playlist")
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error removing video: ${e.message}")
                _message.emit("Erreur lors du retrait")
            }
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _activePlaylist.value = playlist
        if (playlist != null) {
            loadPlaylistVideos(playlist.id)
        } else {
            _playlistVideos.value = emptyList()
        }
    }
}
