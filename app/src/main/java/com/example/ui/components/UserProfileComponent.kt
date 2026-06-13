package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.User
import com.example.data.models.Video

@Composable
fun UserProfileComponent(
    user: User,
    videos: List<Video>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Avatar + Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(80.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = user.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge()
                    }
                }
                Text(text = user.bio ?: "Bio...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = user.followersCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Followers", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = user.followingCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Following", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Videos Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp) // Adjust as needed
        ) {
            items(videos) { video ->
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(9f/16f).padding(1.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
