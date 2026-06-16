package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.User
import com.example.data.models.Video
import com.example.data.models.Playlist
import com.example.viewmodel.ProfileViewModel
import com.example.viewmodel.PlaylistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ProfileViewModel,
    playlistViewModel: PlaylistViewModel,
    initialQuery: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val videos by viewModel.searchedVideos.collectAsState()
    val searchMetadata by viewModel.searchMetadata.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()

    var searchQuery by remember { mutableStateOf(initialQuery ?: "") }
    var selectedTabIndex by remember { mutableStateOf(if (initialQuery?.startsWith("#") == true) 1 else 0) }

    // Advanced search filter states
    var showFilters by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf<String?>(null) } // "24h", "week", "month", "year"
    var selectedViewsFilter by remember { mutableStateOf<String?>(null) } // "popular", "regular"
    var selectedDurationFilter by remember { mutableStateOf<String?>(null) } // "short", "medium", "long"
    var selectedTagFilter by remember { mutableStateOf("") }
    var selectedMentionFilter by remember { mutableStateOf("") }
    var selectedRelevanceFilter by remember { mutableStateOf<String?>(null) } // "relevance", "date", "views"

    // Dialog state for playlist selection
    var selectedVideoForPlaylist by remember { mutableStateOf<Video?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Trigger search when query or filters change
    LaunchedEffect(
        searchQuery,
        selectedDateFilter,
        selectedViewsFilter,
        selectedDurationFilter,
        selectedTagFilter,
        selectedMentionFilter,
        selectedRelevanceFilter
    ) {
        if (searchQuery.length >= 2) {
            viewModel.searchUsersByName(searchQuery)
            viewModel.searchVideosByQuery(
                query = searchQuery,
                date = selectedDateFilter,
                views = selectedViewsFilter,
                duration = selectedDurationFilter,
                tag = selectedTagFilter.ifBlank { null },
                mention = selectedMentionFilter.ifBlank { null },
                relevance = selectedRelevanceFilter
            )
        } else {
            viewModel.loadAllUsers()
            viewModel.searchVideosByQuery("")
        }
    }

    // Load available playlists on search screen open
    LaunchedEffect(Unit) {
        playlistViewModel.loadPlaylists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recherche Avancée 🐆", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Filtres",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tape de quoi faire vrombir Léo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors()
            )

            // Advanced Filters Drawer Panel
            AnimatedVisibility(visible = showFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Options de Filtrage Avancé",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Date Filter Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Date : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(null to "Tout", "24h" to "24h", "week" to "Semaine", "month" to "Mois").forEach { (code, label) ->
                                    FilterChip(
                                        selected = selectedDateFilter == code,
                                        onClick = { selectedDateFilter = code },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // 2. Views / Popularity Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Vues : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(null to "Toutes", "popular" to "Populaires (+100k)", "regular" to "Standards").forEach { (code, label) ->
                                    FilterChip(
                                        selected = selectedViewsFilter == code,
                                        onClick = { selectedViewsFilter = code },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // 3. Duration Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Durée : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(null to "Toutes", "short" to "Court (<1m)", "medium" to "Moyen", "long" to "Long (+5m)").forEach { (code, label) ->
                                    FilterChip(
                                        selected = selectedDurationFilter == code,
                                        onClick = { selectedDurationFilter = code },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // 4. Tag & Mention Inputs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedTagFilter,
                                onValueChange = { selectedTagFilter = it },
                                placeholder = { Text("Tag (ex: rumba)") },
                                leadingIcon = { Text("#", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = selectedMentionFilter,
                                onValueChange = { selectedMentionFilter = it },
                                placeholder = { Text("Mention (ex: leo)") },
                                leadingIcon = { Text("@", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 5. Relevance / Sorting Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Tri par : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(null to "Pertinence", "date" to "Date", "views" to "Vues").forEach { (code, label) ->
                                    FilterChip(
                                        selected = selectedRelevanceFilter == code,
                                        onClick = { selectedRelevanceFilter = code },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results Navigation Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Utilisateurs") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Vidéos") }
                )
            }

            if (searchQuery.length < 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Saisis au moins 2 caractères pour réveiller Léo 🐆",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (selectedTabIndex == 0) {
                // USER LIST
                if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun utilisateur trouvé.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            searchMetadata?.agentSignature?.let { signature ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified Agent",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                        items(users) { user ->
                            UserItemRow(
                                user = user,
                                onClick = { onNavigateToProfile(user.id) },
                                onFollow = { viewModel.toggleFollowInList(user) },
                                onChat = { onNavigateToChat(user.id) }
                            )
                        }
                    }
                }
            } else {
                // VIDEO GRID WITH PLAYLIST SINK
                if (videos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune vidéo trouvée avec ces filtres.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            searchMetadata?.agentSignature?.let { signature ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
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
                        items(videos) { video ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(9f / 16f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        // WATCH / ADD TO PLAYLIST action trigger
                                        selectedVideoForPlaylist = video
                                        showAddToPlaylistDialog = true
                                    }
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl.ifBlank { video.videoUrl },
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Save to playlist short action chip
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable {
                                            selectedVideoForPlaylist = video
                                            showAddToPlaylistDialog = true
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = "Ajouter à la playlist",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${video.views}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD TO PLAYLIST DIALOG
    if (showAddToPlaylistDialog && selectedVideoForPlaylist != null) {
        AlertDialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                selectedVideoForPlaylist = null
            },
            title = { Text("Enregistrer dans une playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Sélectionne une de tes playlists personnalisées pour y ajouter cette vidéo :",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (playlists.isEmpty()) {
                        Text(
                            text = "Tu n'as aucune playlist pour le moment. Rends-toi sur ton profil pour en créer une !",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable {
                                            playlistViewModel.addVideoToPlaylist(
                                                playlist = playlist,
                                                video = selectedVideoForPlaylist!!
                                            ) {
                                                showAddToPlaylistDialog = false
                                                selectedVideoForPlaylist = null
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FeaturedPlayList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(playlist.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${playlist.videosCount} vidéos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddToPlaylistDialog = false
                    selectedVideoForPlaylist = null
                }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun UserItemRow(
    user: User,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    onChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl ?: com.example.R.drawable.strip_logo,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = user.bio ?: "Aucune description",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
        
        IconButton(
            onClick = onChat,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Chat, contentDescription = "Message", modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onFollow,
            colors = if (user.isFollowing) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (user.isFollowing) "Abonné" else "S'abonner",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
