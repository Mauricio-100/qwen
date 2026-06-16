package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    val parsed = remember(text) { parseMarkdownContent(text) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Text blocks loop
        val blocks = remember(parsed.cleanText) { parsed.cleanText.split("\n") }
        blocks.forEach { block ->
            if (block.isNotBlank()) {
                MarkdownFormattedText(text = block, style = style)
            }
        }

        // Render images
        if (parsed.imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            parsed.imageUrls.forEach { imageUrl ->
                ImagePreviewCard(imageUrl = imageUrl)
            }
        }

        // Render videos
        if (parsed.videoUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            parsed.videoUrls.forEach { videoUrl ->
                VideoPreviewCard(videoUrl = videoUrl)
            }
        }
    }
}

data class ParsedMarkdown(
    val cleanText: String,
    val imageUrls: List<String>,
    val videoUrls: List<String>,
    val links: List<Pair<String, String>>
)

fun parseMarkdownContent(input: String): ParsedMarkdown {
    val imageUrls = mutableListOf<String>()
    val videoUrls = mutableListOf<String>()
    val links = mutableListOf<Pair<String, String>>()
    var tempText = input

    // 1. Extract markdown images: ![description](url)
    val mdImageRegex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
    mdImageRegex.findAll(input).forEach { match ->
        val url = match.groupValues[2]
        if (isVideoUrl(url)) {
            videoUrls.add(url)
        } else {
            imageUrls.add(url)
        }
        tempText = tempText.replace(match.value, "")
    }

    // 2. Extract markdown links: [label](url)
    val mdLinkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
    mdLinkRegex.findAll(tempText).forEach { match ->
        val label = match.groupValues[1]
        val url = match.groupValues[2]
        if (isVideoUrl(url)) {
            videoUrls.add(url)
            tempText = tempText.replace(match.value, "")
        } else if (isImageUrl(url)) {
            imageUrls.add(url)
            tempText = tempText.replace(match.value, "")
        } else {
            links.add(label to url)
            tempText = tempText.replace(match.value, label)
        }
    }

    // 3. Extract pure text urls
    val rawUrlRegex = Regex("https?://[\\w\\d:#@%/;\\$\\(\\)~_\\?\\+-=\\\\\\.&]+")
    rawUrlRegex.findAll(tempText).forEach { match ->
        val url = match.value
        if (isVideoUrl(url)) {
            videoUrls.add(url)
            tempText = tempText.replace(url, "")
        } else if (isImageUrl(url)) {
            imageUrls.add(url)
            tempText = tempText.replace(url, "")
        } else {
            links.add(url to url)
        }
    }

    return ParsedMarkdown(tempText.trim(), imageUrls.distinct(), videoUrls.distinct(), links.distinct())
}

fun isVideoUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.endsWith(".mp4") || 
           lower.endsWith(".mkv") || 
           lower.endsWith(".webm") || 
           lower.endsWith(".mov") || 
           lower.contains("video") || 
           lower.contains("gtv-videos-bucket")
}

fun isImageUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("unsplash.com") || 
           lower.contains("via.placeholder") || 
           lower.endsWith(".jpg") || 
           lower.endsWith(".jpeg") || 
           lower.endsWith(".png") || 
           lower.endsWith(".webp") || 
           lower.endsWith(".gif") ||
           lower.contains("image")
}

@Composable
fun MarkdownFormattedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return

    if (text.startsWith("#") && text.length > 2 && text[1] == ' ') {
        val hashCount = text.takeWhile { it == '#' }.length
        val headerText = text.drop(hashCount).trim()
        val textStyle = when (hashCount) {
            1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        }
        Text(text = headerText, style = textStyle, modifier = modifier.padding(vertical = 4.dp))
        return
    }

    val isListItem = text.startsWith("- ") || text.startsWith("* ")
    val cleanedText = if (isListItem) text.drop(2) else text

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    val annotatedString = buildAnnotatedString {
        if (isListItem) {
            append("• ")
        }
        
        var currentIndex = 0
        // Combined regex for Bold, Mentions and Tags
        val complexRegex = Regex("\\\\\\*\\\\\\*(.*?)\\\\\\*\\\\\\*|__(.*?)__|(@[\\\\w.]+)|(#[\\\\w.]+)")
        val matches = complexRegex.findAll(cleanedText).toList()
        
        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(cleanedText.substring(currentIndex, match.range.first))
            }
            
            if (match.groupValues[1].isNotEmpty() || match.groupValues[2].isNotEmpty()) {
                val innerText = match.groupValues[1].ifEmpty { match.groupValues[2] }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(innerText)
                }
            } else if (match.groupValues[3].isNotEmpty()) {
                withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[3])
                }
            } else if (match.groupValues[4].isNotEmpty()) {
                withStyle(SpanStyle(color = secondaryColor, fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[4])
                }
            }
            
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < cleanedText.length) {
            append(cleanedText.substring(currentIndex))
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun ImagePreviewCard(imageUrl: String) {
    var showFullscreenDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(vertical = 6.dp)
            .clickable { showFullscreenDialog = true },
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Image preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
        )
    }
    
    if (showFullscreenDialog) {
        Dialog(
            onDismissRequest = { showFullscreenDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Fullscreen image preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )
                
                IconButton(
                    onClick = { showFullscreenDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }
    }
}

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPreviewCard(videoUrl: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    
    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }
    
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }
    
    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPlaying = !isPlaying }
            )
            
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Lire la vidéo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute/Unmute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
