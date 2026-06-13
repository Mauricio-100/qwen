package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: Int,
    val key: String, // ex: "FIRST_UPLOAD", "TOP_FAN"
    val name: String,
    val description: String,
    val threshold: Int
)

@Entity(tableName = "user_achievements", 
    primaryKeys = ["userId", "achievementId"],
    foreignKeys = [ForeignKey(entity = Achievement::class, parentColumns = ["id"], childColumns = ["achievementId"])]
)
data class UserAchievement(
    val userId: String,
    val achievementId: Int,
    val progress: Int,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM user_achievements")
    fun getUserAchievements(): Flow<List<UserAchievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(userAchievement: UserAchievement)

    @Query("SELECT * FROM user_achievements WHERE achievementId = :achievementId")
    suspend fun getUserAchievement(achievementId: Int): UserAchievement?
}
