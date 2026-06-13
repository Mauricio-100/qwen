package com.example.ui.screens

import com.example.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

import android.content.Intent
import androidx.navigation.NavController
import com.example.ui.navigation.Routes

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel, navController: NavController) {
    val videos by viewModel.videos.collectAsState()
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var showCommentsFor by remember { mutableStateOf<String?>(null) }
    val commentsByVideo by viewModel.videoComments.collectAsState()
    
    // Stories Integration
    val stories by viewModel.stories.collectAsState()
    var activeStoryIndex by remember { mutableIntStateOf(-1) }

    // UI Modal States

    // Optimized Single Shared ExoPlayer for the entire feed
    val context = LocalContext.current
    val attributionContext = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.createAttributionContext("audio")
        } else {
            context
        }
    }
    val sharedExoPlayer = remember {
        ExoPlayer.Builder(attributionContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sharedExoPlayer.release()
        }
    }

    LaunchedEffect(pagerState.currentPage, videos) {
        if (videos.isNotEmpty() && pagerState.currentPage < videos.size) {
            val video = videos[pagerState.currentPage]
            val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
            sharedExoPlayer.setMediaItem(mediaItem)
            sharedExoPlayer.prepare()
            sharedExoPlayer.play()
        }

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
                val video = videos[page]
                VideoPage(
                    video = video,
                    exoPlayer = sharedExoPlayer,
                    isFocused = pagerState.currentPage == page,
                    onLike = { viewModel.likeVideo(video.id) },
                    onCommentClick = {
                        showCommentsFor = video.id
                        viewModel.loadComments(video.id)
                    },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Regardez cette vidéo de ${video.username} sur STRIP : ${video.videoUrl}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                        viewModel.shareVideo(context, video)
                    },
                    onDownloadClick = {
                        viewModel.downloadVideo(video)
                        Toast.makeText(context, "Téléchargement commencé...", Toast.LENGTH_SHORT).show()
                    },
                    onProfileClick = {
                        navController.navigate("${Routes.USER_PROFILE}/${video.userId}")
                    },
                    onFollowClick = {
                        viewModel.followUser(video.userId)
                    },
                    onSaveSound = {
                        viewModel.saveSound(video.id)
                        Toast.makeText(context, "Son ajouté à votre bibliothèque 🎵", Toast.LENGTH_SHORT).show()
                    },
                    onHashtagClick = { tag ->
                        navController.navigate("${Routes.FIND_FRIENDS}?q=${tag}")
                    },
                    onMentionClick = { mention ->
                        val username = if (mention.startsWith("@")) mention.substring(1) else mention
                        navController.navigate("${Routes.FIND_FRIENDS}?q=${username}")
                    }
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
                model = story.avatarUrl ?: R.drawable.strip_logo,
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
fun VideoPage(
    video: Video,
    exoPlayer: ExoPlayer,
    isFocused: Boolean,
    onLike: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFollowClick: () -> Unit,
    onSaveSound: () -> Unit,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isDownloadMenuExpanded by remember { mutableStateOf(false) }
    
    // Heart animation on double tap
    var showHeart by remember { mutableStateOf(false) }
    
    // Rotating music icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "music_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isFocused) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(video.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!video.liked) onLike()
                                showHeart = true
                            },
                            onTap = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        )
                    }
            )
        } else {
            // Lazy loading preview with thumbnail
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Animated Heart on double tap
        if (showHeart) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(100.dp)
                )
            }
            LaunchedEffect(showHeart) {
                delay(800)
                showHeart = false
            }
        }

        // Gradient overlay at bottom of video page to keep overlay text crisp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
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
                Text(
                    text = "@${video.username}", 
                    color = Color.White, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onProfileClick() }
                )
                if (video.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.components.VerifiedBadge(size = 18.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Surface(
                    onClick = { 
                        onFollowClick()
                    },
                    color = if (video.isFollowing) Color.Transparent else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    border = if (video.isFollowing) BorderStroke(1.dp, Color.White) else null,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (video.isFollowing) "Abonné" else "S'abonner",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            com.example.ui.components.FormattedText(
                text = video.description,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick
            )
            
            val firstUrl = remember(video.description) {
                Regex("(https?://[\\\\w.\\-]+[^\\\\s]*)", RegexOption.IGNORE_CASE)
                    .find(video.description)?.value
            }
            if (!firstUrl.isNullOrBlank()) {
                com.example.ui.components.OpenGraphPreview(
                    url = firstUrl,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Audio Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${video.audioTitle ?: "Son original"} - ${video.audioOwner ?: video.username}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
        }

        // Action Buttons Overlaid on the right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    AsyncImage(
                        model = video.avatarUrl ?: R.drawable.strip_logo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (!video.isFollowing) {
                        IconButton(
                            onClick = { 
                                onFollowClick()
                            },
                            modifier = Modifier
                                .offset(y = 10.dp)
                                .size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                IconButton(onClick = onLike, modifier = Modifier.testTag("like_button")) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Like",
                        tint = if (video.liked) Color.Red else Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(text = formatCount(video.likes), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCommentClick) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(text = formatCount(video.commentsCount), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Filled.Reply, 
                        contentDescription = "Share", 
                        tint = Color.White, 
                        modifier = Modifier.size(38.dp).graphicsLayer(scaleX = -1f)
                    )
                }
                Text(text = formatCount(video.sharesCount), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Download & Save Sound Menu
            Box {
                IconButton(onClick = { isDownloadMenuExpanded = true }) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                DropdownMenu(
                    expanded = isDownloadMenuExpanded,
                    onDismissRequest = { isDownloadMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Enregistrer la vidéo") },
                        leadingIcon = { Icon(Icons.Default.VideoFile, contentDescription = null) },
                        onClick = {
                            isDownloadMenuExpanded = false
                            onDownloadClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Télécharger l'audio") },
                        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        onClick = {
                            isDownloadMenuExpanded = false
                            Toast.makeText(context, "Audio en cours de téléchargement...", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copier le lien") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        onClick = {
                            isDownloadMenuExpanded = false
                            Toast.makeText(context, "Lien copié !", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            
            // Rotating Record & Save Sound
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onSaveSound() }
                    .graphicsLayer(rotationZ = rotation)
                    .background(Color.DarkGray, CircleShape)
                    .border(8.dp, Color.Black.copy(alpha = 0.8f), CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.avatarUrl ?: R.drawable.strip_logo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

fun formatCount(count: Int): String {
    return if (count >= 1000) "${String.format("%.1f", count / 1000f)}k" else count.toString()
}
