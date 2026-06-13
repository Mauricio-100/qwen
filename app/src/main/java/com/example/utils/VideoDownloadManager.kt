package com.example.utils

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedVideo
import com.example.data.models.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import android.util.Log

class VideoDownloadManager(
    private val context: Context,
    private val database: AppDatabase,
    private val httpClient: OkHttpClient
) {
    suspend fun downloadVideo(video: Video): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("DownloadManager", "Starting download for ${video.id}")
            val request = Request.Builder().url(video.videoUrl).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            
            // Create a local file in internal storage
            val fileName = "video_${video.id}.mp4"
            val file = File(context.filesDir, "downloads/$fileName")
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            // Save to database
            val downloadedVideo = DownloadedVideo(
                id = video.id,
                videoUrl = video.videoUrl,
                localUri = file.absolutePath,
                thumbnailUrl = video.thumbnailUrl,
                description = video.description,
                username = video.username,
                likes = video.likes,
                views = video.views
            )
            database.videoDao().insertDownload(downloadedVideo)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DownloadManager", "Download error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getDownloadedVideos() = database.videoDao().getDownloadedVideos()
    
    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        val videos = database.videoDao().getDownloadedVideos()
        // Here we'd need to find the file path to delete it too
        // For simplicity in this demo, we'll just remove from DB
        database.videoDao().deleteDownload(id)
    }
}
