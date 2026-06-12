package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.api.RetrofitClient
import com.example.data.api.WebSocketManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.StripRepository
import com.example.ui.navigation.MainApp
import com.example.ui.navigation.Routes
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FeedViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var db: AppDatabase
    private lateinit var retrofitClient: RetrofitClient
    private lateinit var wsManager: WebSocketManager
    private lateinit var repository: StripRepository
    private lateinit var notificationHelper: NotificationHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        prefs = PreferencesManager(this)
        db = Room.databaseBuilder(this, AppDatabase::class.java, "strip_db")
            .fallbackToDestructiveMigration()
            .build()
        retrofitClient = RetrofitClient(this, prefs)
        wsManager = WebSocketManager(OkHttpClient())
        notificationHelper = NotificationHelper(this)
        
        val downloadManager = com.example.utils.VideoDownloadManager(this, db, OkHttpClient())

        repository = StripRepository(
            apiService = retrofitClient.apiService,
            wsManager = wsManager,
            db = db,
            prefs = prefs,
            notificationHelper = notificationHelper,
            downloadManager = downloadManager
        )

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) return AuthViewModel(repository) as T
                if (modelClass.isAssignableFrom(FeedViewModel::class.java)) return FeedViewModel(repository) as T
                if (modelClass.isAssignableFrom(com.example.viewmodel.ChatViewModel::class.java)) return com.example.viewmodel.ChatViewModel(repository) as T
                if (modelClass.isAssignableFrom(com.example.viewmodel.ProfileViewModel::class.java)) return com.example.viewmodel.ProfileViewModel(repository) as T
                if (modelClass.isAssignableFrom(com.example.viewmodel.ActFileViewModel::class.java)) return com.example.viewmodel.ActFileViewModel(repository) as T
                throw IllegalArgumentException("Unknown ViewModel")
            }
        }

        // Initial content while loading state
        setContent {
            val isDarkThemePref by prefs.isDarkThemeFlow.collectAsState(initial = null)
            val darkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = darkTheme) {
                var startDestination by remember { mutableStateOf<String?>(null) }
                val token by prefs.tokenFlow.collectAsState(initial = null)

                LaunchedEffect(token) {
                    if (!token.isNullOrEmpty()) {
                        repository.connectWebSocket()
                        startDestination = Routes.FEED
                        
                        // Real-Time WebSocket Listener for Notifications
                        launch {
                            repository.wsManager.messageEvents.collect { json ->
                                val type = json.optString("type")
                                if (type == "notification") {
                                    val message = json.optString("message")
                                    val title = json.optString("title", "STRIP - Nouveau")
                                    notificationHelper.showNotification(
                                        title = title,
                                        message = message
                                    )
                                } else if (type == "new_follower") {
                                    notificationHelper.showNotification(
                                        title = "STRIP",
                                        message = "Vous avez un nouvel abonné !"
                                    )
                                }
                            }
                        }
                    } else {
                        startDestination = Routes.AUTH
                    }
                }

                startDestination?.let { dest ->
                    MainApp(
                        authViewModel = ViewModelProvider(this@MainActivity, factory)[AuthViewModel::class.java],
                        feedViewModel = ViewModelProvider(this@MainActivity, factory)[FeedViewModel::class.java],
                        chatViewModel = ViewModelProvider(this@MainActivity, factory)[com.example.viewmodel.ChatViewModel::class.java],
                        profileViewModel = ViewModelProvider(this@MainActivity, factory)[com.example.viewmodel.ProfileViewModel::class.java],
                        actFileViewModel = ViewModelProvider(this@MainActivity, factory)[com.example.viewmodel.ActFileViewModel::class.java],
                        startDestination = dest
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
