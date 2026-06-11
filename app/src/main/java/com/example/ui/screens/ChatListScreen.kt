package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.Conversation
import com.example.viewmodel.ChatViewModel

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyRow

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val stories by viewModel.stories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (stories.isNotEmpty() || true) { // Always show row even if empty for mock adding
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // "Add Story" button
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* TODO */ }) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Mon statut", style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(stories) { story ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* TODO View Story */ }) {
                        AsyncImage(
                            model = story.avatarUrl ?: "https://via.placeholder.com/150",
                            contentDescription = "Story",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(story.username, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(conversations) { conversation ->
                ConversationItem(conversation = conversation, onClick = { onNavigateToChat(conversation.userId) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = conversation.avatarUrl ?: "https://via.placeholder.com/150",
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = conversation.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (conversation.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerifiedBadge(size = 16.dp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1
            )
        }
        if (conversation.unreadCount > 0) {
            Badge { Text(conversation.unreadCount.toString()) }
        }
    }
}
