package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.WebSocketManager
import com.example.data.local.AppDatabase
import com.example.data.local.CachedVideo
import com.example.data.local.PreferencesManager
import com.example.data.local.toCachedEntity
import com.example.data.local.toDomainModel
import com.example.data.models.Video
import com.example.data.models.User
import com.example.data.models.Message
import com.example.data.models.Conversation
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine

class StripRepository(
    val apiService: ApiService,
    val wsManager: WebSocketManager,
    val db: AppDatabase,
    val prefs: PreferencesManager,
    val notificationHelper: com.example.NotificationHelper,
    val downloadManager: com.example.utils.VideoDownloadManager? = null,
    val networkMonitor: NetworkMonitor
) {
    val isOfflineFlow: Flow<Boolean> = combine(
        networkMonitor.isOnline,
        prefs.offlineSimFlow
    ) { online, simOffline ->
        !online || simOffline
    }

    suspend fun getFeed(cursor: String? = null): Result<List<Video>> {
        val offline = isOfflineFlow.first()
        if (offline) {
            val cached = db.videoDao().getAllVideos().first()
            return if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainModel() })
            } else {
                Result.failure(Exception("Aucune vidéo en cache disponible hors ligne"))
            }
        }
        return try {
            val response = apiService.getFeed(cursor = cursor)
            if (cursor == null) {
                // Cache the initial load
                db.videoDao().insertVideos(response.map {
                    CachedVideo(
                        id = it.id,
                        videoUrl = it.videoUrl,
                        thumbnailUrl = it.thumbnailUrl,
                        description = it.description,
                        username = it.username,
                        likes = it.likes,
                        views = it.views
                    )
                })
            }
            Result.success(response)
        } catch (e: Exception) {
            // try fallback list
            val cached = db.videoDao().getAllVideos().first()
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomainModel() }) else Result.failure(e)
        }
    }

    suspend fun getFollowingFeed(cursor: String? = null): Result<List<Video>> {
        val offline = isOfflineFlow.first()
        if (offline) {
            val cached = db.videoDao().getAllVideos().first()
            return if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainModel() })
            } else {
                Result.failure(Exception("Suivi non disponible hors ligne"))
            }
        }
        return try {
            val response = apiService.getFollowingFeed(cursor = cursor)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<User> {
        val offline = isOfflineFlow.first()
        if (offline) {
            val cached = db.cachedDao().getUserProfile(userId)
            return if (cached != null) {
                Result.success(cached.toDomainModel())
            } else {
                Result.failure(Exception("Profil utilisateur non disponible hors ligne"))
            }
        }
        return try {
            val response = apiService.getUserProfile(userId)
            db.cachedDao().insertUserProfile(response.toCachedEntity())
            Result.success(response)
        } catch (e: Exception) {
            val cached = db.cachedDao().getUserProfile(userId)
            if (cached != null) Result.success(cached.toDomainModel()) else Result.failure(e)
        }
    }

    suspend fun getMyProfile(): Result<User> {
        val offline = isOfflineFlow.first()
        if (offline) {
            val myId = prefs.userIdFlow.first() ?: ""
            if (myId.isNotEmpty()) {
                val cached = db.cachedDao().getUserProfile(myId)
                if (cached != null) return Result.success(cached.toDomainModel())
            }
            return Result.failure(Exception("Profil non disponible hors ligne"))
        }
        return try {
            val response = apiService.getMyProfile()
            db.cachedDao().insertUserProfile(response.toCachedEntity())
            Result.success(response)
        } catch (e: Exception) {
            val myId = prefs.userIdFlow.first() ?: ""
            val cached = if (myId.isNotEmpty()) db.cachedDao().getUserProfile(myId) else null
            if (cached != null) Result.success(cached.toDomainModel()) else Result.failure(e)
        }
    }

    suspend fun getMessages(userId: String): Result<List<Message>> {
        val offline = isOfflineFlow.first()
        val myId = prefs.userIdFlow.first() ?: ""
        if (offline) {
            val cached = db.cachedDao().getMessages(myId, userId).first()
            return if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainModel() })
            } else {
                Result.failure(Exception("Messages non disponibles hors ligne"))
            }
        }
        return try {
            val response = apiService.getMessages(userId)
            db.cachedDao().insertMessages(response.map { it.toCachedEntity() })
            Result.success(response)
        } catch (e: Exception) {
            val cached = db.cachedDao().getMessages(myId, userId).first()
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomainModel() }) else Result.failure(e)
        }
    }

    suspend fun getConversations(): Result<List<Conversation>> {
        val offline = isOfflineFlow.first()
        if (offline) {
            val cached = db.cachedDao().getConversations()
            return if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainModel() })
            } else {
                Result.failure(Exception("Conversations non disponibles hors ligne"))
            }
        }
        return try {
            val response = apiService.getConversations()
            db.cachedDao().insertConversations(response.map { it.toCachedEntity() })
            Result.success(response)
        } catch (e: Exception) {
            val cached = db.cachedDao().getConversations()
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomainModel() }) else Result.failure(e)
        }
    }

    fun getCachedFeed(): Flow<List<CachedVideo>> {
        return db.videoDao().getAllVideos()
    }

    suspend fun connectWebSocket() {
        if (isOfflineFlow.first()) return // Skip websocket when offline
        val userId = prefs.userIdFlow.first()
        if (userId != null) {
            wsManager.connect(userId)
        }
    }

    fun disconnectWebSocket() {
        wsManager.disconnect()
    }

    suspend fun insertLocalVideo(video: Video) {
        db.videoDao().insertVideos(listOf(
            com.example.data.local.CachedVideo(
                id = video.id,
                videoUrl = video.videoUrl,
                thumbnailUrl = video.thumbnailUrl,
                description = video.description,
                username = video.username,
                likes = video.likes,
                views = video.views
            )
        ))
    }

    suspend fun getNotifications(unreadOnly: Boolean = false): Result<List<com.example.data.models.Notification>> {
        val offline = isOfflineFlow.first()
        if (offline) {
            return Result.success(emptyList())
        }
        return try {
            val notifications = apiService.getNotifications(unreadOnly = unreadOnly)
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache() {
        db.cachedDao().clearUserProfiles()
        db.cachedDao().clearMessages()
        db.cachedDao().clearConversations()
        db.videoDao().clearAll()
    }
}
