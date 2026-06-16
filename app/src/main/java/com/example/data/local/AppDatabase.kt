package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_videos")
data class CachedVideo(
    @PrimaryKey val id: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val description: String,
    val username: String,
    val likes: Int,
    val views: Int
)

@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey val id: String,
    val videoUrl: String,
    val localUri: String,
    val thumbnailUrl: String,
    val description: String,
    val username: String,
    val likes: Int,
    val views: Int,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_user_profiles")
data class CachedUserProfile(
    @PrimaryKey val id: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val email: String?,
    val phoneNumber: String?,
    val birthDate: String?,
    val zodiacSign: String?,
    val followersCount: Int,
    val followingCount: Int,
    val videosCount: Int,
    val likesReceived: Int,
    val isFollowing: Boolean
)

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: String,
    val content: String,
    val type: String,
    val senderId: String,
    val receiverId: String,
    val read: Boolean,
    val senderUsername: String,
    val senderAvatar: String?,
    val isVerified: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_conversations")
data class CachedConversation(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val lastMessage: String?,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isVerified: Boolean
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM cached_videos")
    fun getAllVideos(): Flow<List<CachedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<CachedVideo>)

    @Query("DELETE FROM cached_videos")
    suspend fun clearAll()

    @Query("SELECT * FROM downloaded_videos ORDER BY downloadedAt DESC")
    fun getDownloadedVideos(): Flow<List<DownloadedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(video: DownloadedVideo)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteDownload(id: String)
}

@Dao
interface CachedDao {
    @Query("SELECT * FROM cached_user_profiles WHERE id = :userId")
    suspend fun getUserProfile(userId: String): CachedUserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: CachedUserProfile)

    @Query("DELETE FROM cached_user_profiles")
    suspend fun clearUserProfiles()

    @Query("SELECT * FROM cached_messages WHERE (senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId) ORDER BY timestamp ASC")
    fun getMessages(myId: String, otherId: String): Flow<List<CachedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("DELETE FROM cached_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM cached_conversations")
    fun getConversationsFlow(): Flow<List<CachedConversation>>

    @Query("SELECT * FROM cached_conversations")
    suspend fun getConversations(): List<CachedConversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<CachedConversation>)

    @Query("DELETE FROM cached_conversations")
    suspend fun clearConversations()
}

@Database(
    entities = [
        CachedVideo::class,
        DownloadedVideo::class,
        Achievement::class,
        UserAchievement::class,
        CachedUserProfile::class,
        CachedMessage::class,
        CachedConversation::class,
        LocalPlaylist::class,
        LocalPlaylistVideo::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun achievementDao(): AchievementDao
    abstract fun cachedDao(): CachedDao
    abstract fun playlistDao(): LocalPlaylistDao
}
