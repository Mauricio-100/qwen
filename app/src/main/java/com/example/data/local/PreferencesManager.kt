package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USERNAME_KEY = stringPreferencesKey("username")
        val THEME_MODE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("theme_mode_dark")
        val OFFLINE_SIM_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("offline_sim_mode")
        val MAX_CACHE_SIZE_KEY = androidx.datastore.preferences.core.intPreferencesKey("max_cache_size")
        val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
    }

    suspend fun saveAuth(token: String, userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USERNAME_KEY] = username
        }
    }

    suspend fun setThemeMode(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = isDark
        }
    }

    suspend fun setOfflineSimMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[OFFLINE_SIM_KEY] = enabled
        }
    }

    suspend fun setMaxCacheSize(megabytes: Int) {
        context.dataStore.edit { prefs ->
            prefs[MAX_CACHE_SIZE_KEY] = megabytes
        }
    }

    suspend fun setDownloadQuality(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[DOWNLOAD_QUALITY_KEY] = quality
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USERNAME_KEY)
        }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }
    val isDarkThemeFlow: Flow<Boolean?> = context.dataStore.data.map { it[THEME_MODE_KEY] }
    
    val offlineSimFlow: Flow<Boolean> = context.dataStore.data.map { it[OFFLINE_SIM_KEY] ?: false }
    val maxCacheSizeFlow: Flow<Int> = context.dataStore.data.map { it[MAX_CACHE_SIZE_KEY] ?: 500 }
    val downloadQualityFlow: Flow<String> = context.dataStore.data.map { it[DOWNLOAD_QUALITY_KEY] ?: "Medium" }

    suspend fun getToken(): String? {
        return tokenFlow.first()
    }
}
