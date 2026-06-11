package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.WebSocketManager
import com.example.data.local.AppDatabase
import com.example.data.local.CachedVideo
import com.example.data.local.PreferencesManager
import com.example.data.models.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CmoRepository(
    val apiService: ApiService,
    val wsManager: WebSocketManager,
    private val db: AppDatabase,
    val prefs: PreferencesManager
) {
    suspend fun getFeed(cursor: String? = null): Result<List<Video>> {
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
            Result.failure(e)
        }
    }

    fun getCachedFeed(): Flow<List<CachedVideo>> {
        return db.videoDao().getAllVideos()
    }

    suspend fun connectWebSocket() {
        val userId = prefs.userIdFlow.first()
        if (userId != null) {
            wsManager.connect(userId)
        }
    }

    fun disconnectWebSocket() {
        wsManager.disconnect()
    }
}
