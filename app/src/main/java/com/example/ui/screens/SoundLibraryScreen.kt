package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.R
import com.example.data.models.Sound
import com.example.ui.navigation.Routes
import com.example.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundLibraryScreen(
    viewModel: FeedViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val recommendedSounds by viewModel.recommendedSounds.collectAsState()
    val categorySounds by viewModel.categorySounds.collectAsState()
    val searchedSounds by viewModel.searchedSounds.collectAsState()
    val searchMetadata by viewModel.searchMetadata.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Amapiano") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Recommandations, 1 = Par Catégorie, 2 = Recherche

    // ExoPlayer state for live sound preview
    var playingSoundId by remember { mutableStateOf<String?>(null) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        playingSoundId = null
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        viewModel.loadRecommendedSounds()
        viewModel.loadSoundsByCategory(selectedCategory)
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(selectedCategory) {
        viewModel.loadSoundsByCategory(selectedCategory)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            viewModel.searchSounds(searchQuery)
        }
    }

    fun playPreview(sound: Sound) {
        if (playingSoundId == sound.id) {
            exoPlayer.pause()
            playingSoundId = null
        } else {
            playingSoundId = sound.id
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(sound.audioUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bibliothèque de Sons", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.length >= 2) {
                        selectedTab = 2
                    } else if (it.isEmpty() && selectedTab == 2) {
                        selectedTab = 0
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Rechercher un artiste, titre, remix...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            selectedTab = 0
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Tabs for navigation within library
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Populaires 🎵", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Genres 🎸", fontWeight = FontWeight.SemiBold) }
                )
                if (searchQuery.length >= 2) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Résultats 🔍", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Recommendations tab
                    if (recommendedSounds.isEmpty()) {
                        EmptyLibraryState("Aucun son recommandé pour le moment. Soyez le premier à ajouter une piste !")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(recommendedSounds) { sound ->
                                SoundLibraryRow(
                                    sound = sound,
                                    isPlaying = playingSoundId == sound.id,
                                    onPlayClick = { playPreview(sound) },
                                    onSelectClick = {
                                        viewModel.selectSound(sound)
                                        navController.popBackStack()
                                    },
                                    onDetailsClick = {
                                        navController.navigate("${Routes.SOUND_PIVOT}/${sound.id}")
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Category sounds tab
                    Column(modifier = Modifier.weight(1f)) {
                        val categories = listOf("Amapiano", "Afrorap", "Gospel", "Rumba", "Électro", "Hip-Hop", "Podcast", "Acoustique")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (categorySounds.isEmpty()) {
                            EmptyLibraryState("Aucun son dans la catégorie \"$selectedCategory\". Proposez vos propres pistes !")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(categorySounds) { sound ->
                                    SoundLibraryRow(
                                        sound = sound,
                                        isPlaying = playingSoundId == sound.id,
                                        onPlayClick = { playPreview(sound) },
                                        onSelectClick = {
                                            viewModel.selectSound(sound)
                                            navController.popBackStack()
                                        },
                                        onDetailsClick = {
                                            navController.navigate("${Routes.SOUND_PIVOT}/${sound.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Custom search results
                    if (searchedSounds.isEmpty()) {
                        EmptyLibraryState("Aucun son correspondant à votre recherche.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                searchMetadata?.agentSignature?.let { signature ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verified Agent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = signature,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            items(searchedSounds) { sound ->
                                SoundLibraryRow(
                                    sound = sound,
                                    isPlaying = playingSoundId == sound.id,
                                    onPlayClick = { playPreview(sound) },
                                    onSelectClick = {
                                        viewModel.selectSound(sound)
                                        navController.popBackStack()
                                    },
                                    onDetailsClick = {
                                        navController.navigate("${Routes.SOUND_PIVOT}/${sound.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundLibraryRow(
    sound: Sound,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onSelectClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Artwork Cover
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            AsyncImage(
                model = sound.coverUrl ?: R.drawable.strip_logo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = sound.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "par @${sound.authorUsername}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${sound.usesCount} vidéos • ${sound.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Select and Action Button
        Button(
            onClick = onSelectClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Utiliser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyLibraryState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


