package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FeedScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FeedViewModel

import androidx.compose.material.icons.filled.Email
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ProfileScreen
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ProfileViewModel
import com.example.viewmodel.ActFileViewModel
import com.example.ui.screens.ActFilesScreen
import com.example.ui.screens.ChatDetailScreen
import androidx.compose.material.icons.filled.List

import com.example.ui.screens.PublishScreen

object Routes {
    const val AUTH = "auth"
    const val FEED = "feed"
    const val CHAT = "chat"
    const val CHAT_DETAIL = "chat_detail"
    const val PROFILE = "profile"
    const val USER_PROFILE = "user_profile"
    const val ACTFILES = "actfiles"
    const val VERIFICATION = "verification"
    const val PUBLISH = "publish"
    const val NOTIFICATIONS = "notifications"
    const val FIND_FRIENDS = "find_friends"
    const val SOUND_PIVOT = "sound_pivot"
    const val SOUND_LIBRARY = "sound_library"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist_detail"
}

@Composable
fun MainApp(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    chatViewModel: ChatViewModel,
    profileViewModel: ProfileViewModel,
    actFileViewModel: ActFileViewModel,
    playlistViewModel: com.example.viewmodel.PlaylistViewModel,
    startDestination: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOffline by profileViewModel.isOfflineFlow.collectAsState(initial = false)

    val context = androidx.compose.ui.platform.LocalContext.current
    val requiredPermissions = remember {
        val list = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }
    
    var showPermissionCard by remember {
        mutableStateOf(
            requiredPermissions.any {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        showPermissionCard = false
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.AUTH && currentRoute?.startsWith(Routes.CHAT_DETAIL) == false && currentRoute != Routes.PUBLISH) {
                BottomNavigationBar(
                    navController, 
                    currentRoute, 
                    onPublishClick = { navController.navigate(Routes.PUBLISH) },
                    onFeedClick = {
                        if (currentRoute == Routes.FEED) {
                            feedViewModel.loadFeed()
                        } else {
                            navController.navigate(Routes.FEED)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.AUTH) {
                    AuthScreen(viewModel = authViewModel, onLoginSuccess = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    })
                }
                composable(Routes.FEED) {
                    FeedScreen(viewModel = feedViewModel, navController = navController)
                }
                composable(Routes.CHAT) {
                    ChatListScreen(
                        viewModel = chatViewModel,
                        feedViewModel = feedViewModel,
                        onNavigateToChat = { userId ->
                            navController.navigate("${Routes.CHAT_DETAIL}/$userId")
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        onNavigateToFindFriends = {
                            navController.navigate(Routes.FIND_FRIENDS)
                        }
                    )
                }
                composable("${Routes.CHAT_DETAIL}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    ChatDetailScreen(viewModel = chatViewModel, userId = userId, onNavigateBack = { navController.popBackStack() })
                }
                composable(Routes.NOTIFICATIONS) {
                    com.example.ui.screens.NotificationsScreen(
                        viewModel = profileViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "${Routes.FIND_FRIENDS}?q={initialQuery}",
                    arguments = listOf(androidx.navigation.navArgument("initialQuery") { 
                        type = androidx.navigation.NavType.StringType
                        nullable = true 
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("initialQuery")
                    com.example.ui.screens.SearchScreen(
                        viewModel = profileViewModel,
                        playlistViewModel = playlistViewModel,
                        initialQuery = query,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToChat = { userId ->
                            navController.navigate("${Routes.CHAT_DETAIL}/$userId")
                        },
                        onNavigateToProfile = { userId ->
                            navController.navigate("${Routes.USER_PROFILE}/$userId")
                        }
                    )
                }
                composable(Routes.PLAYLISTS) {
                    com.example.ui.screens.playlist.PlaylistsScreen(
                        viewModel = playlistViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate("${Routes.PLAYLIST_DETAIL}/$playlistId")
                        }
                    )
                }
                composable("${Routes.PLAYLIST_DETAIL}/{playlistId}") { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    com.example.ui.screens.playlist.PlaylistDetailScreen(
                        viewModel = playlistViewModel,
                        playlistId = playlistId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.ACTFILES) {
                    ActFilesScreen(
                        viewModel = actFileViewModel, 
                        feedViewModel = feedViewModel,
                        onNavigateToPublish = { navController.navigate(Routes.PUBLISH) },
                        onNavigateToProfile = { userId -> navController.navigate("${Routes.USER_PROFILE}/$userId") }
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        navController = navController,
                        onNavigateToVerification = { navController.navigate(Routes.VERIFICATION) }
                    )
                }
                composable("${Routes.USER_PROFILE}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    ProfileScreen(
                        viewModel = profileViewModel,
                        userId = userId,
                        navController = navController,
                        onNavigateToVerification = { navController.navigate(Routes.VERIFICATION) }
                    )
                }
                composable(Routes.VERIFICATION) {
                    com.example.ui.screens.VerificationScreen(viewModel = profileViewModel)
                }
                composable(Routes.PUBLISH) {
                    PublishScreen(
                        feedViewModel = feedViewModel,
                        actFileViewModel = actFileViewModel,
                        navController = navController,
                        onDismiss = { navController.popBackStack() }
                    )
                }
                composable("${Routes.SOUND_PIVOT}/{soundId}") { backStackEntry ->
                    val soundId = backStackEntry.arguments?.getString("soundId") ?: return@composable
                    com.example.ui.screens.SoundPivotScreen(
                        soundId = soundId,
                        viewModel = feedViewModel,
                        navController = navController
                    )
                }
                composable(Routes.SOUND_LIBRARY) {
                    com.example.ui.screens.SoundLibraryScreen(
                        viewModel = feedViewModel,
                        navController = navController
                    )
                }
            }

            if (isOffline) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Mode Hors-ligne",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Mode Hors-ligne (Léo veille 🐆)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showPermissionCard,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.85f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = R.drawable.img_leopard_mascot),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Donne ton Feu Vert ! 🐆",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Pour publier des vidéos, profiter d'un son fluide de haute fidélité et recevoir les alertes instantanées de Léo, STRIP requiert ces accès :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Caméra & Vidéo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Permet de capturer tes swipes et d'interagir", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Mic, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Microphone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Pour enregistrer tes sons avec une clarté totale", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Notifications push", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Reçois des messages et des relances de la mascotte", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Accorder les Autorisations", fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                TextButton(
                                    onClick = { showPermissionCard = false },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Plus tard", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?, onPublishClick: () -> Unit, onFeedClick: () -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Feed") },
            selected = currentRoute == Routes.FEED,
            onClick = onFeedClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "ActFiles") },
            label = { Text("ActFiles") },
            selected = currentRoute == Routes.ACTFILES,
            onClick = {
                if (currentRoute != Routes.ACTFILES) navController.navigate(Routes.ACTFILES)
            }
        )
        NavigationBarItem(
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publier", tint = Color.White)
                }
            },
            label = { Text("Créer") },
            selected = false,
            onClick = onPublishClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Email, contentDescription = "Chat") },
            label = { Text("Chat") },
            selected = currentRoute == Routes.CHAT,
            onClick = {
                if (currentRoute != Routes.CHAT) navController.navigate(Routes.CHAT)
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == Routes.PROFILE,
            onClick = {
                if (currentRoute != Routes.PROFILE) navController.navigate(Routes.PROFILE)
            }
        )
    }
}
