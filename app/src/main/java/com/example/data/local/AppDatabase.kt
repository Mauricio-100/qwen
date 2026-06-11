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

@Dao
interface VideoDao {
    @Query("SELECT * FROM cached_videos")
    fun getAllVideos(): Flow<List<CachedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<CachedVideo>)

    @Query("DELETE FROM cached_videos")
    suspend fun clearAll()
}

@Database(entities = [CachedVideo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
