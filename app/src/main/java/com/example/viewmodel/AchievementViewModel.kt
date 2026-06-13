package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.Achievement
import com.example.data.local.AchievementDao
import com.example.data.local.UserAchievement
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AchievementUiState(
    val achievements: List<Achievement> = emptyList(),
    val userAchievements: List<UserAchievement> = emptyList()
)

class AchievementViewModel(private val dao: AchievementDao) : ViewModel() {

    val uiState: StateFlow<AchievementUiState> = combine(
        dao.getAllAchievements(),
        dao.getUserAchievements()
    ) { achievements, userAchievements ->
        AchievementUiState(achievements, userAchievements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementUiState())

    fun updateProgress(achievementId: Int, progress: Int, userId: String) {
        viewModelScope.launch {
            val current = dao.getUserAchievement(achievementId)
            val newProgress = if (current != null) current.progress + progress else progress
            dao.insertUserAchievement(UserAchievement(userId, achievementId, newProgress, newProgress >= 100))
        }
    }
}
