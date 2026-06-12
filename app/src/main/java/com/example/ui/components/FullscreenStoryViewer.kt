package com.example.ui.components

import android.net.Uri
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.models.Story
import com.example.viewmodel.FeedViewModel
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun FullscreenStoryViewer(
    stories: List<Story>,
    initialIndex: Int,
    onAddComment: (String, String) -> Unit = { _, _ -> }, // storyId, content
    onIncrementView: (String) -> Unit = {},
    commentsProvider: @Composable (String) -> List<com.example.data.models.VideoComment> = { emptyList() },
    viewsProvider: @Composable (String) -> Int = { 0 },
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    var progress by remember { mutableFloatStateOf(0f) }
    var replyText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }

    val comments = commentsProvider(currentStory.id)

    // Increment views when active item shifts
    LaunchedEffect(currentStory.id) {
        onIncrementView(currentStory.id)
    }

    // Progression timer
    LaunchedEffect(currentIndex) {
        progress = 0f
        while (progress < 1f) {
            val paused = isPaused || replyText.isNotEmpty()
            if (!paused) {
                delay(50L) // 100 steps * 50ms = 5000ms (5 seconds)
                progress += 0.01f
            } else {
                delay(100L) // await unpaused
            }
        }
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val isVideo = remember(currentStory.mediaUrl) {
                val url = currentStory.mediaUrl.lowercase()
                url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".webm") || url.contains("video") || url.contains("gtv-videos-bucket")
            }

            if (isVideo) {
                val context = LocalContext.current
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                    }
                }

                LaunchedEffect(currentStory.mediaUrl) {
                    val mediaItem = MediaItem.fromUri(Uri.parse(currentStory.mediaUrl))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }

                DisposableEffect(currentStory.mediaUrl) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Immersive Media Image
                AsyncImage(
                    model = currentStory.mediaUrl,
                    contentDescription = "Story media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // Hold-to-pause and Back/Next Tap screen area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(stories.size) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val isLeft = offset.x < size.width * 0.35f
                                if (isLeft) {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                    } else {
                                        onDismiss()
                                    }
                                } else {
                                    if (currentIndex < stories.size - 1) {
                                        currentIndex++
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
            )

            // Overlaid controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // High-End active timer bars at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val itemProgress = when {
                            idx < currentIndex -> 1f
                            idx > currentIndex -> 0f
                            else -> progress
                        }
                        LinearProgressIndicator(
                            progress = { itemProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Author Info & Close row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentStory.avatarUrl ?: R.drawable.strip_logo,
                        contentDescription = "Story avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentStory.username,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentStory.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                com.example.ui.components.VerifiedBadge(size = 14.dp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentStory.createdAt,
                                color = Color.LightGray,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Vues",
                                tint = Color.LightGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val count = viewsProvider(currentStory.id)
                            Text(
                                text = count.toString(),
                                color = Color.LightGray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Story",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Scrollable Live Comments sliding list above the input box
                if (comments.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .heightIn(max = 140.dp)
                            .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Commentaires",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(comments) { comment ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    AsyncImage(
                                        model = comment.avatarUrl ?: R.drawable.strip_logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comment.username,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comment.content,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Reply comment area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Écrire un commentaire...", color = Color.LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 1,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotEmpty()) {
                                onAddComment(currentStory.id, replyText)
                                replyText = ""
                            }
                        },
                        enabled = replyText.isNotEmpty(),
                        modifier = Modifier
                            .background(
                                color = if (replyText.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Envoyer le commentaire",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
