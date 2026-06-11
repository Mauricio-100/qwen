package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Message
import com.example.viewmodel.ChatViewModel

@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    userId: String
) {
    val messages by viewModel.messages.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    var currentMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        viewModel.loadMessages(userId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(messages) { message ->
                MessageBubble(message = message, isMe = message.senderUsername == currentUsername)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentMessage,
                onValueChange = { currentMessage = it },
                modifier = Modifier.weight(1f).testTag("message_input"),
                placeholder = { Text("Tapez un message...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (currentMessage.isNotBlank()) {
                        viewModel.sendMessage(userId, currentMessage, currentUsername)
                        currentMessage = ""
                    }
                },
                modifier = Modifier.testTag("send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Envoyer")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isMe) MaterialTheme.colorScheme.primary else Color.LightGray,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .widthIn(max = 250.dp)
        ) {
            Column {
                if (!isMe) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.senderUsername,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                        if (message.isVerified) {
                            Spacer(modifier = Modifier.width(2.dp))
                            com.example.ui.components.VerifiedBadge(size = 12.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else Color.Black
                )
            }
        }
    }
}
