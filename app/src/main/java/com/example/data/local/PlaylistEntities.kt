package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_playlists")
data class LocalPlaylist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "local_playlist_videos",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = LocalPlaylist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["videoId"])]
)
data class LocalPlaylistVideo(
    val playlistId: String,
    val videoId: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val description: String,
    val username: String,
    val likes: Int,
    val views: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPlaylistsFlow(userId: String): Flow<List<LocalPlaylist>>

    @Query("SELECT * FROM local_playlists WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPlaylists(userId: String): List<LocalPlaylist>

    @Query("SELECT * FROM local_playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): LocalPlaylist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylist)

    @Query("DELETE FROM local_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(playlistVideo: LocalPlaylistVideo)

    @Query("DELETE FROM local_playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistVideo(playlistId: String, videoId: String)

    @Query("SELECT * FROM local_playlist_videos WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getPlaylistVideosFlow(playlistId: String): Flow<List<LocalPlaylistVideo>>

    @Query("SELECT * FROM local_playlist_videos WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    suspend fun getPlaylistVideos(playlistId: String): List<LocalPlaylistVideo>

    @Query("SELECT COUNT(*) FROM local_playlist_videos WHERE playlistId = :playlistId")
    fun getVideosCountFlow(playlistId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_playlist_videos WHERE playlistId = :playlistId")
    suspend fun getVideosCount(playlistId: String): Int
}
