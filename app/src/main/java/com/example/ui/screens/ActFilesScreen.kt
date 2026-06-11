package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.Comment
import com.example.ui.components.MarkdownText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.ActFile
import com.example.viewmodel.ActFileViewModel

@Composable
fun ActFilesScreen(viewModel: ActFileViewModel) {
    val actFiles by viewModel.actFiles.collectAsState()
    var newPostContent by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newPostContent,
                onValueChange = { newPostContent = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Quoi de neuf ? (Markdown)") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { 
                if (newPostContent.isNotBlank()) {
                    viewModel.createActFile(newPostContent)
                    newPostContent = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Publier")
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(actFiles) { file ->
                ActFileItem(file = file, onLike = { viewModel.likeActFile(file.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ActFileItem(file: ActFile, onLike: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        AsyncImage(
            model = file.avatarUrl ?: "https://via.placeholder.com/150",
            contentDescription = "Avatar",
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = file.username, fontWeight = FontWeight.Bold)
                if (file.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerifiedBadge()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            MarkdownText(text = file.content)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (file.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (file.liked) Color.Red else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.likesCount.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Comment, contentDescription = "Replies", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.repliesCount.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "Views", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.viewsCount.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}
