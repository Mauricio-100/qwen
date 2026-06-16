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
import com.example.data.models.formatAudioLabel
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke

import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.ui.navigation.Routes

fun getFolderSize(dir: java.io.File): Long {
    var size: Long = 0
    val files = dir.listFiles()
    if (files != null) {
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
    }
    return size
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: String? = null,
    navController: NavController? = null,
    onNavigateToVerification: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = if (userId == null) {
        listOf("Profil", "Mes Vidéos", "Téléchargements", "Sons", "Playlists", "Amis", "Statistiques")
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
                            viewModel.updateAvatar(context, uri)
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
                
                user.zodiacSign?.let { zodiac ->
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(text = getZodiacEmoji(zodiac), fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
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
                
                // Calculate dynamic stats for 'real metadata' based on loaded videos
                val userVideos by viewModel.myVideos.collectAsState()
                val realLikeCount = if (userId != null) userVideos.sumOf { it.likes }.coerceAtLeast(user.likesReceived) else user.likesReceived
                val videosCountStat = if (userId != null) userVideos.size.coerceAtLeast(user.videosCount) else user.videosCount
                val userSounds by viewModel.userSounds.collectAsState()
                val soundsCountStat = userSounds.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Abonnés", value = user.followersCount.toString())
                    StatItem(label = "Abonnements", value = user.followingCount.toString())
                    StatItem(label = "J'aimes", value = realLikeCount.toString())
                    StatItem(label = "Vidéos", value = videosCountStat.toString())
                    StatItem(label = "Sons", value = soundsCountStat.toString())
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
                    currentTabTitle == "Sons" -> UserSoundsTab(viewModel, navController)
                    currentTabTitle == "Playlists" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FeaturedPlayList,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Mes Playlists Personnalisées",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Organise, filtre et sauvegarde tes swings favoris dans tes dossiers privés.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navController?.navigate(Routes.PLAYLISTS) }
                                ) {
                                    Text("Gérer mes playlists")
                                }
                            }
                        }
                    }
                    currentTabTitle == "Amis" -> FriendsTab(viewModel, navController)
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
    var bio by remember { mutableStateOf(user.bio ?: "") }

    val isDarkThemePref by viewModel.isDarkThemeFlow.collectAsState(initial = null)
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = isDarkThemePref ?: isSystemDark
    val context = androidx.compose.ui.platform.LocalContext.current

    val isSimOffline by viewModel.offlineSimFlow.collectAsState(initial = false)
    val downloadQuality by viewModel.downloadQualityFlow.collectAsState(initial = "Medium")
    val isAppOffline by viewModel.isOfflineFlow.collectAsState(initial = false)

    var folderSizeMB by remember { mutableStateOf(0.0) }

    LaunchedEffect(isAppOffline) {
        val dir = java.io.File(context.filesDir, "downloads")
        folderSizeMB = if (dir.exists()) {
            getFolderSize(dir).toDouble() / (1024.0 * 1024.0)
        } else {
            0.0
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // LEOPARD MASCOT HERO CHAT BUBBLE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_leopard_mascot),
                            contentDescription = "Mascotte Léo le Léopard",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Léo le Léopard 🐆",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isAppOffline) {
                                    "Aïe, pas d'internet ou mode simulation ! 🐆 Mais pas de panique, je veille sur tes vidéos, messages et profils en cache (jusqu'à 500Mo) !"
                                } else {
                                    "Raaawr! Bienvenue sur STRIP! 🐆 Tout fonctionne à merveille. Je pré-cache tes swipes préférés automatiquement !"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // OFFLINE SETTINGS CONTROLLERS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paramètres Hors-Ligne 🐆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulate Offline Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simuler Mode Hors-ligne",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Force l'app en mode déconnectée pour tester le cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSimOffline,
                            onCheckedChange = { viewModel.setOfflineSimMode(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Quality Choice
                    Text(
                        text = "Qualité de téléchargement",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Basse", "Moyenne", "Haute").forEach { quality ->
                            val isSelected = downloadQuality.lowercase() == quality.lowercase()
                            InputChip(
                                selected = isSelected,
                                onClick = { viewModel.setDownloadQuality(quality) },
                                label = { Text(quality) },
                                modifier = Modifier.weight(1f),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage and 500MB Limit Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cache Utilisé (Limite 500Mo)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = String.format("%.2f Mo / 500 Mo", folderSizeMB),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (folderSizeMB / 500.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCacheAndDownloads(context) {
                                folderSizeMB = 0.0
                                android.widget.Toast.makeText(context, "Cache vidé !", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vider le cache hors-ligne")
                    }
                }
            }
        }

        // GENERAL THEME & USER FORM
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode Sombre",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(checked = isDarkTheme, onCheckedChange = { viewModel.setThemeMode(it) })
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Numéro") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dob, 
                onValueChange = { dob = it }, 
                label = { Text("Date de Naissance (Format JJ/MM/AA ou AAAA-MM-JJ)") }, 
                placeholder = { Text("Ex: 15/05/98 ou 1998-05-15") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Description (Bio)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val normalizedDob = normalizeBirthDate(dob)
                    viewModel.updateProfile(mapOf(
                        "email" to email,
                        "phone_number" to phone,
                        "birth_date" to normalizedDob,
                        "bio" to bio
                    ))
                    if (normalizedDob.isNotBlank()) {
                        viewModel.requestVerification(normalizedDob)
                    }
                    android.widget.Toast.makeText(context, "Profil mis à jour !", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
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
fun FriendsTab(viewModel: ProfileViewModel, navController: NavController? = null) {
    val users by viewModel.allUsers.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            viewModel.searchUsersByName(query)
        } else {
            // Initiate default populate search
            viewModel.searchUsersByName("a")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Rechercher des utilisateurs") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        if (users.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aucun utilisateur trouvé", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                lazyItems(users) { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController?.navigate("${Routes.USER_PROFILE}/${u.id}")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = u.avatarUrl ?: R.drawable.strip_logo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (u.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    com.example.ui.components.VerifiedBadge(size = 14.dp)
                                }
                            }
                            Text(
                                text = u.bio ?: "Aucune description",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.toggleFollowInList(u)
                            },
                            colors = if (u.isFollowing) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (u.isFollowing) "Abonné" else "S'abonner",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
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
fun UserSoundsTab(viewModel: ProfileViewModel, navController: NavController? = null) {
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
                        .clickable {
                            val soundId = sound.soundId ?: "original_${sound.id}"
                            navController?.navigate("${Routes.SOUND_PIVOT}/$soundId")
                        }
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
                            text = sound.formatAudioLabel(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
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

fun getZodiacEmoji(sign: String): String {
    return when (sign.lowercase()) {
        "bélier" -> "Bélier ♈"
        "taureau" -> "Taureau ♉"
        "gémeaux" -> "Gémeaux ♊"
        "cancer" -> "Cancer ♋"
        "lion" -> "Lion ♌"
        "vierge" -> "Vierge ♍"
        "balance" -> "Balance ♎"
        "scorpion" -> "Scorpion ♏"
        "sagittaire" -> "Sagittaire ♐"
        "capricorne" -> "Capricorne ♑"
        "verseau" -> "Verseau ♒"
        "poissons" -> "Poissons ♓"
        else -> sign
    }
}

fun normalizeBirthDate(input: String): String {
    val trimmed = input.trim()
    
    // Match YY/MM/DD or YY-MM-DD or YY.MM.DD
    val yyMatch = Regex("^(\\d{2})[/.-](\\d{2})[/.-](\\d{2})$").find(trimmed)
    if (yyMatch != null) {
        val (yy, mm, dd) = yyMatch.destructured
        val yearPrefix = if (yy.toInt() > 40) "19" else "20"
        return "$yearPrefix$yy-$mm-$dd"
    }
    
    // Match DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
    val dmyMatch = Regex("^(\\d{2})[/.-](\\d{2})[/.-](\\d{4})$").find(trimmed)
    if (dmyMatch != null) {
        val (dd, mm, yyyy) = dmyMatch.destructured
        return "$yyyy-$mm-$dd"
    }
    
    // Match YYYY/MM/DD or YYYY-MM-DD or YYYY.MM.DD
    val ymdMatch = Regex("^(\\d{4})[/.-](\\d{2})[/.-](\\d{2})$").find(trimmed)
    if (ymdMatch != null) {
        val (yyyy, mm, dd) = ymdMatch.destructured
        return "$yyyy-$mm-$dd"
    }
    
    return trimmed
}
