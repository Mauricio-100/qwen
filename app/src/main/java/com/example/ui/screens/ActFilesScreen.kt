package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.example.data.models.ActFile
import com.example.viewmodel.ActFileViewModel
import com.example.viewmodel.FeedViewModel
import androidx.compose.material.icons.automirrored.filled.Comment
import com.example.ui.components.FullscreenStoryViewer
import com.example.ui.components.MarkdownText
import com.example.R
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun ActFilesScreen(
    viewModel: ActFileViewModel,
    feedViewModel: FeedViewModel
) {
    val actFiles by viewModel.actFiles.collectAsState()
    val stories by feedViewModel.stories.collectAsState()
    var newPostContent by remember { mutableStateOf("") }
    
    var activeStoryIndex by remember { mutableIntStateOf(-1) }
    var showPublishStoryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
            // High-End Squares horizontal stories ribbon
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(
                        text = "Stories Récentes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "Ma Story" Publish button card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(120.dp)
                                    .clickable { showPublishStoryDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Partager une story",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Ma Story",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Users standard square stories cards
                        itemsIndexed(stories) { index, story ->
                            Card(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(120.dp)
                                    .clickable { activeStoryIndex = index },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Silent visual preview
                                    AsyncImage(
                                        model = story.mediaUrl,
                                        contentDescription = "Story Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Gradient shadow overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                                )
                                            )
                                    )
                                    
                                    // Circular small avatar
                                    AsyncImage(
                                        model = story.avatarUrl ?: R.drawable.strip_logo,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .align(Alignment.TopStart)
                                    )

                                    // Glassmorphic views badge at top-end (right)
                                    val storyViewsMap by feedViewModel.storyViews.collectAsState()
                                    val views = storyViewsMap[story.id] ?: feedViewModel.getStoryViews(story.id)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Vues",
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = views.toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    // Display name bottom text
                                    Text(
                                        text = story.username,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                            .align(Alignment.BottomStart)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                }
            }

            // ActFiles List Items
            items(actFiles) { file ->
                ActFileItem(file = file, viewModel = viewModel, onLike = { viewModel.likeActFile(file.id) })
                HorizontalDivider()
            }
        }
    }

    // Modal Publish Dialog trigger
    if (showPublishStoryDialog) {
        com.example.ui.components.PublishBottomSheet(
            feedViewModel = feedViewModel,
            actFileViewModel = viewModel,
            onDismiss = { showPublishStoryDialog = false }
        )
    }

    // Full screen interactive story viewer window
    if (activeStoryIndex in stories.indices) {
        FullscreenStoryViewer(
            stories = stories,
            initialIndex = activeStoryIndex,
            onAddComment = { id, content -> feedViewModel.addStoryComment(id, content) },
            onIncrementView = { id -> feedViewModel.incrementStoryView(id) },
            commentsProvider = { id -> feedViewModel.storyComments.collectAsState().value[id] ?: emptyList() },
            viewsProvider = { id -> feedViewModel.storyViews.collectAsState().value[id] ?: feedViewModel.getStoryViews(id) },
            onDismiss = { activeStoryIndex = -1 }
        )
    }
}

@Composable
fun ActFileItem(
    file: ActFile,
    viewModel: ActFileViewModel,
    onLike: () -> Unit
) {
    var showReplies by remember { mutableStateOf(false) }

    LaunchedEffect(file.id) {
        viewModel.incrementActFileViews(file.id)
    }

    LaunchedEffect(showReplies) {
        if (showReplies) {
            viewModel.loadReplies(file.id)
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        AsyncImage(
            model = file.avatarUrl ?: R.drawable.strip_logo,
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
                            imageVector = if (file.liked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (file.liked) Color.Red else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.likesCount.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showReplies = !showReplies }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Comment, contentDescription = "Replies", tint = if (showReplies) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.repliesCount.toString(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (showReplies) FontWeight.Bold else FontWeight.Normal), color = if (showReplies) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "Views", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = file.viewsCount.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            if (showReplies) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val repliesMap by viewModel.replies.collectAsState()
                val replies = repliesMap[file.id] ?: emptyList()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Réponses (${file.repliesCount})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (replies.isEmpty()) {
                        Text(
                            text = "Aucune réponse pour le moment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            replies.forEach { reply ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    AsyncImage(
                                        model = reply.avatarUrl ?: R.drawable.strip_logo,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = reply.username,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (reply.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                com.example.ui.components.VerifiedBadge(size = 12.dp)
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = reply.createdAt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = reply.content,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Reply Box Input
                    var replyText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Écrire une réponse...", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    viewModel.addReply(file.id, replyText)
                                    replyText = ""
                                }
                            },
                            enabled = replyText.isNotBlank(),
                            modifier = Modifier
                                .background(
                                    color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Répondre",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
