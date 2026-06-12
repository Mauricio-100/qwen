package com.example.ui.screens

import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.ProfileViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.models.Video
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: String? = null,
    onNavigateToVerification: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = if (userId == null) {
        listOf("Profil", "Mes Vidéos", "Téléchargements", "Sons", "Amis", "Statistiques")
    } else {
        listOf("Vidéos", "Sons")
    }

    LaunchedEffect(userId) {
        if (userId == null) {
            viewModel.loadMyProfile()
            viewModel.loadVerificationStatus()
            viewModel.loadMyVideos()
            viewModel.loadMySavedSounds()
            viewModel.loadAllUsers()
            viewModel.loadServerStats()
        } else {
            viewModel.loadUserProfile(userId)
            viewModel.loadUserVideos(userId)
            viewModel.loadUserSounds(userId)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    userProfile?.let { user ->
        Column(modifier = Modifier.fillMaxSize()) {
            // App Bar with Logout
            if (userId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        viewModel.logout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val outlineColor = MaterialTheme.colorScheme.primary

                    val avatarPickerLauncher = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            // Update avatar URL locally using the URI
                            viewModel.updateProfile(mapOf("avatar_url" to uri.toString()))
                        }
                    }

                    AsyncImage(
                        model = user.avatarUrl ?: R.drawable.strip_logo,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                    if (userId == null) {
                        IconButton(
                            onClick = { 
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = user.username, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (user.isVerified || user.badge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        com.example.ui.components.VerifiedBadge(size = 24.dp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = user.bio ?: "Aucune bio", style = MaterialTheme.typography.bodyLarge)
                
                user.zodiacSign?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Signe : $it", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                
                if (!user.isVerified && userId == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viewModel.verificationStatus.collectAsState().value != null) {
                        Button(onClick = onNavigateToVerification) {
                            Text("Critères de certification")
                        }
                    } else {
                       CircularProgressIndicator()
                    }
                }

                if (userId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.followUser(userId)
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (user.isFollowing) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors()
                        ) {
                            Text(if (user.isFollowing) "Abonné" else "S'abonner")
                        }
                        OutlinedButton(
                            onClick = { /* Navigate to chat */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Abonnés", value = user.followersCount.toString())
                    StatItem(label = "Abonnements", value = user.followingCount.toString())
                    StatItem(label = "J'aimes", value = user.likesReceived.toString())
                }
            }
            
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val currentTabTitle = tabs[selectedTab]
                when {
                    currentTabTitle == "Profil" -> ProfileInfoTab(user, viewModel)
                    currentTabTitle == "Mes Vidéos" || currentTabTitle == "Vidéos" -> MyVideosTab(viewModel)
                    currentTabTitle == "Téléchargements" -> DownloadedVideosTab(viewModel)
                    currentTabTitle == "Sons" -> UserSoundsTab(viewModel)
                    currentTabTitle == "Amis" -> FriendsTab(viewModel)
                    currentTabTitle == "Statistiques" -> StatsTab(viewModel)
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun DownloadedVideosTab(viewModel: ProfileViewModel) {
    val videos by viewModel.downloadedVideos.collectAsState(initial = emptyList())
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucun téléchargement") }
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            items(videos) { video ->
                Box(modifier = Modifier.aspectRatio(9f/16f).padding(2.dp)) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                    )
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoTab(user: com.example.data.models.User, viewModel: ProfileViewModel) {
    var email by remember { mutableStateOf(user.email ?: "") }
    var phone by remember { mutableStateOf(user.phoneNumber ?: "") }
    var dob by remember { mutableStateOf(user.birthDate ?: "") }

    val isDarkThemePref by viewModel.isDarkThemeFlow.collectAsState(initial = null)
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = isDarkThemePref ?: isSystemDark

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mode Sombre")
                Switch(checked = isDarkTheme, onCheckedChange = { viewModel.setThemeMode(it) })
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Numéro") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date de Naissance") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                viewModel.updateProfile(mapOf(
                    "email" to email,
                    "phone_number" to phone,
                    "birth_date" to dob
                ))
            }) {
                Text("Sauvegarder")
            }
        }
    }
}

@Composable
fun MyVideosTab(viewModel: ProfileViewModel) {
    val videos by viewModel.myVideos.collectAsState()
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucune vidéo") }
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            items(videos) { video ->
                Box(modifier = Modifier.aspectRatio(9f/16f).padding(2.dp)) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                    )
                    Text(
                        video.views.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsTab(viewModel: ProfileViewModel) {
    val users by viewModel.allUsers.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        lazyItems(users) { u ->
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = u.avatarUrl ?: R.drawable.strip_logo, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Text(u.username, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun StatsTab(viewModel: ProfileViewModel) {
    val stats by viewModel.serverStats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Mes Statistiques Réelles", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (userProfile != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Abonnement(s)", fontWeight = FontWeight.Bold)
                    Text(userProfile!!.followingCount.toString())
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Abonné(s)", fontWeight = FontWeight.Bold)
                    Text(userProfile!!.followersCount.toString())
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total J'aimes reçus", fontWeight = FontWeight.Bold)
                    Text(userProfile!!.likesReceived.toString())
                }
                HorizontalDivider()
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Statistiques Globales du Serveur", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (stats != null) {
            stats!!.forEach { (k, v) ->
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, fontWeight = FontWeight.Bold)
                        Text(v.toString())
                    }
                    HorizontalDivider()
                }
            }
        } else {
            item { CircularProgressIndicator() }
        }
    }
}

@Composable
fun UserSoundsTab(viewModel: ProfileViewModel) {
    val sounds by viewModel.userSounds.collectAsState()
    if (sounds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Aucun son disponible 🎵", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sounds) { sound ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.audioTitle ?: "Son original",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Par @${sound.audioOwner ?: sound.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* Play Preview */ }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}
