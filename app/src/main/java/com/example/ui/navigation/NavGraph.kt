package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

object Routes {
    const val AUTH = "auth"
    const val FEED = "feed"
    const val CHAT = "chat"
    const val CHAT_DETAIL = "chat_detail"
    const val PROFILE = "profile"
    const val ACTFILES = "actfiles"
    const val VERIFICATION = "verification"
}

@Composable
fun MainApp(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    chatViewModel: ChatViewModel,
    profileViewModel: ProfileViewModel,
    actFileViewModel: ActFileViewModel,
    startDestination: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showPublishBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.AUTH && currentRoute?.startsWith(Routes.CHAT_DETAIL) == false) {
                BottomNavigationBar(navController, currentRoute, onPublishClick = { showPublishBottomSheet = true })
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
                    FeedScreen(viewModel = feedViewModel)
                }
                composable(Routes.CHAT) {
                    ChatListScreen(
                        viewModel = chatViewModel,
                        feedViewModel = feedViewModel,
                        onNavigateToChat = { userId ->
                            navController.navigate("${Routes.CHAT_DETAIL}/$userId")
                        }
                    )
                }
                composable("${Routes.CHAT_DETAIL}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    ChatDetailScreen(viewModel = chatViewModel, userId = userId, onNavigateBack = { navController.popBackStack() })
                }
                composable(Routes.ACTFILES) {
                    ActFilesScreen(viewModel = actFileViewModel, feedViewModel = feedViewModel)
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToVerification = { navController.navigate(Routes.VERIFICATION) }
                    )
                }
                composable(Routes.VERIFICATION) {
                    com.example.ui.screens.VerificationScreen(viewModel = profileViewModel)
                }
            }
            
            if (showPublishBottomSheet) {
                com.example.ui.components.PublishBottomSheet(
                    feedViewModel = feedViewModel,
                    actFileViewModel = actFileViewModel,
                    onDismiss = { showPublishBottomSheet = false }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?, onPublishClick: () -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Feed") },
            selected = currentRoute == Routes.FEED,
            onClick = {
                if (currentRoute != Routes.FEED) navController.navigate(Routes.FEED)
            }
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
