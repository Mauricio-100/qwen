package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.models.Video
import com.example.viewmodel.FeedViewModel

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val videos by viewModel.videos.collectAsState()
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var showCommentsFor by remember { mutableStateOf<String?>(null) }
    val commentsByVideo by viewModel.videoComments.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= videos.size - 2) {
            viewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                VideoPage(
                    video = videos[page],
                    isFocused = pagerState.currentPage == page,
                    onLike = { viewModel.likeVideo(videos[page].id) },
                    onCommentClick = {
                        showCommentsFor = videos[page].id
                        viewModel.loadComments(videos[page].id)
                    },
                    onShareClick = {
                        viewModel.shareVideo(videos[page].id)
                    },
                    onDownloadClick = { /* Simulation of download */ }
                )
            }

            if (showCommentsFor != null) {
                ModalBottomSheet(onDismissRequest = { showCommentsFor = null }) {
                    CommentsSheet(
                        comments = commentsByVideo[showCommentsFor] ?: emptyList(),
                        onAddComment = { content ->
                            viewModel.addComment(showCommentsFor!!, content)
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Publication")
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Nouvelle publication") },
                text = { Text("MOCK: Fonctionnalité de téléchargement de vidéo en développement.") },
                confirmButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun CommentsSheet(comments: List<com.example.data.models.VideoComment>, onAddComment: (String) -> Unit) {
    var newComment by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).padding(16.dp)) {
        Text("Commentaires", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f)) {
            items(comments) { comment ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(text = comment.username, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    if (comment.isVerified) {
                       Spacer(modifier = Modifier.width(4.dp))
                       com.example.ui.components.VerifiedBadge(size = 14.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = comment.content)
                }
                HorizontalDivider()
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ajouter un commentaire...") }
            )
            IconButton(onClick = {
                if (newComment.isNotBlank()) {
                    onAddComment(newComment)
                    newComment = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPage(video: Video, isFocused: Boolean, onLike: () -> Unit, onCommentClick: () -> Unit, onShareClick: () -> Unit, onDownloadClick: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(video.videoUrl) {
        val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
            exoPlayer.seekTo(0)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize().clickable {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "@${video.username}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (video.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerifiedBadge(size = 18.dp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = video.description, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onLike, modifier = Modifier.testTag("like_button")) {
                Icon(
                    imageVector = if (video.liked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (video.liked) Color.Red else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(text = video.likes.toString(), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            IconButton(onClick = onCommentClick) {
                Icon(imageVector = Icons.Default.Message, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(text = video.commentsCount.toString(), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            IconButton(onClick = onShareClick) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(text = video.sharesCount.toString(), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onDownloadClick) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
