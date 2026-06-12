package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.models.Video
import com.example.data.models.Story
import com.example.ui.components.FullscreenStoryViewer
import com.example.viewmodel.FeedViewModel
import kotlinx.coroutines.delay

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val videos by viewModel.videos.collectAsState()
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var showCommentsFor by remember { mutableStateOf<String?>(null) }
    val commentsByVideo by viewModel.videoComments.collectAsState()
    
    // Stories Integration
    val stories by viewModel.stories.collectAsState()
    var activeStoryIndex by remember { mutableIntStateOf(-1) }

    // UI Modal States
    var showPublishBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (videos.isNotEmpty() && pagerState.currentPage >= videos.size - 2) {
            viewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    onDownloadClick = { /* No-op */ }
                )
            }

            // Top Overlay for Stories
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(vertical = 12.dp)
            ) {
                if (stories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(stories) { index, story ->
                            StoryCircle(
                                story = story,
                                onClick = { activeStoryIndex = index }
                            )
                        }
                    }
                }
            }

            // Unchanged media details layout without top-bar stories row

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

        if (showPublishBottomSheet) {
            com.example.ui.components.PublishBottomSheet(
                feedViewModel = viewModel,
                actFileViewModel = null,
                onDismiss = { showPublishBottomSheet = false }
            )
        }

        // End of dialog overlays
        if (activeStoryIndex in stories.indices) {
            FullscreenStoryViewer(
                stories = stories,
                initialIndex = activeStoryIndex,
                onAddComment = { id, content -> viewModel.addStoryComment(id, content) },
                onIncrementView = { id -> viewModel.incrementStoryView(id) },
                commentsProvider = { id -> viewModel.storyComments.collectAsState().value[id] ?: emptyList() },
                viewsProvider = { id -> viewModel.storyViews.collectAsState().value[id] ?: viewModel.getStoryViews(id) },
                onDismiss = { activeStoryIndex = -1 }
            )
        }
    }
}

@Composable
fun CommentsSheet(comments: List<com.example.data.models.VideoComment>, onAddComment: (String) -> Unit) {
    var newComment by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).padding(16.dp).imePadding()) {
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

@Composable
fun StoryCircle(story: Story, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            AsyncImage(
                model = story.avatarUrl ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = story.username,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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

        // Gradient overlay at bottom of video page to keep overlay text crisp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "@${video.username}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (video.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerifiedBadge(size = 18.dp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = video.description, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        }

        // Action Buttons Overlaid on the right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 12.dp),
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
            Text(text = video.likes.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            IconButton(onClick = onCommentClick) {
                Icon(imageVector = Icons.Default.Message, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(text = video.commentsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            IconButton(onClick = onShareClick) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(text = video.sharesCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onDownloadClick) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
