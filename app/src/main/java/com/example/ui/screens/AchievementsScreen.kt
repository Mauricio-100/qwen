package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewmodel.AchievementViewModel

@Composable
fun AchievementsScreen(viewModel: AchievementViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Mes Achievements", style = MaterialTheme.typography.headlineMedium) }
        items(state.achievements) { achievement ->
            val userAch = state.userAchievements.find { it.achievementId == achievement.id }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(achievement.name, style = MaterialTheme.typography.titleMedium)
                    Text(achievement.description, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { (userAch?.progress ?: 0).toFloat() / achievement.threshold },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
