package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Message
import com.example.data.models.Conversation
import com.example.data.models.User
import com.example.viewmodel.ChatViewModel
import com.example.ui.components.MarkdownText
import com.example.ui.components.VerifiedBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    userId: String,
    onNavigateBack: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val reactions by viewModel.messageReactions.collectAsState()
    val selectedWallpaper by viewModel.selectedWallpaper.collectAsState()

    var currentMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Sheets & Dialog states
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    
    // Reaction selection local anchor
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        viewModel.loadMessages(userId)
    }

    // Identify current active interlocutor details
    val activeInterlocutor = remember(conversations, contacts, userId) {
        conversations.find { it.userId == userId }?.let {
            User(id = it.id, username = it.username, avatarUrl = it.avatarUrl, isOnline = it.isOnline, isVerified = it.isVerified)
        } ?: contacts.find { it.id == userId }
    }

    // Wallpaper configuration colors
    val backgroundBrush = remember(selectedWallpaper) {
        when (selectedWallpaper) {
            "cosmique" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            )
            "emerald" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0F2027), Color(0xFF134E5E), Color(0xFF71B280))
            )
            "neon" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF141E30), Color(0xFF243B55))
            )
            "luxe" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1D976C), Color(0xFF93F9B9))
            )
            else -> Brush.verticalGradient( // Default pleasant high contrast light layout
                colors = listOf(Color(0xFFF5F7FA), Color(0xFFE4E8F0))
            )
        }
    }

    val themeOnBackground = remember(selectedWallpaper) {
        if (selectedWallpaper == "default") Color.Black else Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Tap to view profile */ }
                    ) {
                        Box {
                            AsyncImage(
                                model = activeInterlocutor?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                contentDescription = "Active chat avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            if (activeInterlocutor?.isOnline == true) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(1.5.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF2ECC71))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeInterlocutor?.username ?: "Discussion",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeInterlocutor?.isVerified == true) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(size = 14.dp)
                                }
                            }
                            
                            Text(
                                text = if (activeInterlocutor?.isOnline == true) "En ligne actuellement" else "Hors ligne",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeInterlocutor?.isOnline == true) Color(0xFF2ECC71) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showWallpaperDialog = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Changer l'arrière-plan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Chats contents scroll
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true
                ) {
                    // Let's add a spacing placeholder at the very end (meaning bottom of messages list)
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Display list of messages
                    items(messages.reversed()) { message ->
                        val isMe = message.senderUsername == currentUsername || message.senderId == "me"
                        val userReaction = reactions[message.id]

                        MessageRowBlock(
                            message = message,
                            isMe = isMe,
                            reaction = userReaction,
                            themeOnBackground = themeOnBackground,
                            onLongClick = {
                                activeReactionMessageId = message.id
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Inputs field layout area
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media plus sharing button
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Attachments", modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text input
                        OutlinedTextField(
                            value = currentMessage,
                            onValueChange = { currentMessage = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input"),
                            placeholder = { Text("Écrire un message...", color = Color.Gray, fontSize = 14.sp) },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send button
                        IconButton(
                            onClick = {
                                if (currentMessage.isNotBlank()) {
                                    viewModel.sendMessage(userId, currentMessage, currentUsername)
                                    currentMessage = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (currentMessage.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currentMessage.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color.Gray
                            ),
                            enabled = currentMessage.isNotBlank(),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("send_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Envoyer", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // FLOATING EMOJI REACTION POPUP OVERLAY
            if (activeReactionMessageId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { activeReactionMessageId = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier
                            .padding(32.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Réagir au message",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val emojis = listOf("❤️", "👍", "😂", "😮", "😢", "👏")
                                emojis.forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.reactToMessage(activeReactionMessageId!!, emoji)
                                                activeReactionMessageId = null
                                            }
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGWALLPAPERS SELECTOR
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Personnaliser votre Chat", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choisissez un fond d'écran immersif pour vos conversations :", color = Color.Gray, fontSize = 13.sp)
                    val presets = listOf(
                        "default" to "Classique Minimal (Clair)",
                        "cosmique" to "Cosmos Saphir (Sombre)",
                        "emerald" to "Forêt d'Émeraude",
                        "neon" to "Cyberpunk Néon",
                        "luxe" to "Menthe Dorée"
                    )
                    presets.forEach { (tag, label) ->
                        val isSelected = selectedWallpaper == tag
                        Button(
                            onClick = {
                                viewModel.changeWallpaper(tag)
                                showWallpaperDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) { Text("Annuler") }
            }
        )
    }

    // ATTACHMENT MODAL SHEET
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Partager un contenu média",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Générez un lien markdown interactif pour enrichir la conversation :",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Grid options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Send simulated photography
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentMessage = "Regarde cette superbe image ! ![Nature](https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80)"
                                showAttachmentSheet = false
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Photo", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Partager Photo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    // Send simulated video player
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentMessage = "Regarde ce clip vidéo ! [Big Buck Bunny](https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4)"
                                showAttachmentSheet = false
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Vidéo", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Partager Vidéo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRowBlock(
    message: Message,
    isMe: Boolean,
    reaction: String?,
    themeOnBackground: Color,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (!isMe) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderUsername,
                        style = MaterialTheme.typography.labelSmall,
                        color = themeOnBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    if (message.isVerified) {
                        Spacer(modifier = Modifier.width(3.dp))
                        VerifiedBadge(size = 11.dp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .background(
                        brush = if (isMe) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF4081), MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.85f))
                            )
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    // Utilise notre composant complet MarkdownText
                    MarkdownText(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = if (isMe) Color.White else Color.Black
                        )
                    )
                }
            }

            // Reaction overlap badge
            if (reaction != null) {
                Box(
                    modifier = Modifier
                        .offset(y = (-8).dp, x = if (isMe) (-8).dp else 8.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = reaction, fontSize = 11.sp)
                }
            }
        }
    }
}
