package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.User
import com.example.viewmodel.ProfileViewModel
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.PlayArrow
import com.example.data.models.Video

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(
    viewModel: ProfileViewModel,
    initialQuery: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val videos by viewModel.searchedVideos.collectAsState()
    
    var searchQuery by remember { mutableStateOf(initialQuery ?: "") }
    var selectedTabIndex by remember { mutableStateOf(if (initialQuery?.startsWith("#") == true) 1 else 0) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            viewModel.searchUsersByName(searchQuery)
            viewModel.searchVideosByQuery(searchQuery)
        } else {
            viewModel.loadAllUsers() // Clears users
            viewModel.searchVideosByQuery("") // Clears videos
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recherche", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors()
            )

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
                    Text("Tapez au moins 2 caractères pour rechercher.")
                }
            } else if (selectedTabIndex == 0) {
                if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun utilisateur trouvé.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            UserItemRow(
                                user = user,
                                onClick = { onNavigateToProfile(user.id) },
                                onFollow = { viewModel.followUser(user.id) },
                                onChat = { onNavigateToChat(user.id) }
                            )
                        }
                    }
                }
            } else {
                if (videos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune vidéo trouvée.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(videos) { video ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(9f / 16f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { /* Could navigate to play video */ }
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl.ifBlank { video.videoUrl },
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
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
        IconButton(
            onClick = onFollow,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Suivre", modifier = Modifier.size(20.dp))
        }
    }
}
