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
import com.example.data.repository.CmoRepository
import com.example.ui.navigation.MainApp
import com.example.ui.navigation.Routes
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FeedViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var db: AppDatabase
    private lateinit var retrofitClient: RetrofitClient
    private lateinit var wsManager: WebSocketManager
    private lateinit var repository: CmoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = PreferencesManager(this)
        db = Room.databaseBuilder(this, AppDatabase::class.java, "cmo_db").build()
        retrofitClient = RetrofitClient(this, prefs)
        wsManager = WebSocketManager(OkHttpClient())
        
        repository = CmoRepository(
            apiService = retrofitClient.apiService,
            wsManager = wsManager,
            db = db,
            prefs = prefs
        )

        // Check if user is logged in
        val token = runBlocking { prefs.getToken() }
        val startDestination = if (token.isNullOrEmpty()) Routes.AUTH else Routes.FEED
        if (!token.isNullOrEmpty()) {
            runBlocking { repository.connectWebSocket() }
        }

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

        val authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        val feedViewModel = ViewModelProvider(this, factory)[FeedViewModel::class.java]
        val chatViewModel = ViewModelProvider(this, factory)[com.example.viewmodel.ChatViewModel::class.java]
        val profileViewModel = ViewModelProvider(this, factory)[com.example.viewmodel.ProfileViewModel::class.java]
        val actFileViewModel = ViewModelProvider(this, factory)[com.example.viewmodel.ActFileViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainApp(
                    authViewModel = authViewModel,
                    feedViewModel = feedViewModel,
                    chatViewModel = chatViewModel,
                    profileViewModel = profileViewModel,
                    actFileViewModel = actFileViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}
