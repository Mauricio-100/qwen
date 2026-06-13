package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.models.User
import com.example.ui.components.VerifiedBadge
import com.example.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isLoadingUsers by viewModel.isLoadingUsers.collectAsState()
    val followInFlightIds by viewModel.followInFlightIds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearErrorMessage()
        }
    }

    val suggestedUsers = remember(users, currentUserId) {
        users
            .filterNot { it.id == currentUserId }
            .sortedWith(
                compareByDescending<User> { it.isOnline }
                    .thenByDescending { it.followersCount }
                    .thenBy { it.username.lowercase() }
            )
    }

    val filteredUsers = remember(suggestedUsers, searchQuery) {
        rankUsersForSearch(suggestedUsers, searchQuery)
    }

    val hasSearch = searchQuery.isNotBlank()
    val resultLabel = if (hasSearch) {
        "${filteredUsers.size} resultat(s) pour \"${searchQuery.trim()}\""
    } else {
        "${suggestedUsers.size} profil(s) a decouvrir"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trouver des amis", fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp),
                placeholder = { Text("Rechercher un utilisateur...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Text(
                text = resultLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoadingUsers && users.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                filteredUsers.isEmpty() -> {
                    EmptySearchState(
                        hasSearch = hasSearch,
                        query = searchQuery,
                        onReset = { searchQuery = "" }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            UserItemCard(
                                user = user,
                                isFollowLoading = followInFlightIds.contains(user.id),
                                onClick = { onNavigateToProfile(user.id) },
                                onFollow = { viewModel.followUser(user.id) },
                                onChat = { onNavigateToChat(user.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItemCard(
    user: User,
    onClick: () -> Unit,
    isFollowLoading: Boolean,
    onFollow: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = user.avatarUrl ?: R.drawable.strip_logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp),
                    shape = CircleShape,
                    color = if (user.isOnline) Color(0xFF2EBD59) else MaterialTheme.colorScheme.outline
                ) {}
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isVerified || user.badge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        VerifiedBadge(size = 18.dp)
                    }
                }

                Text(
                    text = user.bio?.takeIf { it.isNotBlank() }
                        ?: if (user.isOnline) "En ligne maintenant" else "Profil a decouvrir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${user.followersCount} abonnes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
                enabled = !isFollowLoading,
                colors = if (user.isFollowing) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isFollowLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (user.isFollowing) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                } else {
                    Icon(
                        imageVector = if (user.isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (user.isFollowing) "Abonne" else "Suivre")
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    hasSearch: Boolean,
    query: String,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasSearch) "Aucun profil ne correspond a \"$query\"." else "Aucun profil disponible pour le moment.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasSearch) "Essaie un pseudo plus court ou un autre mot-cle." else "Reviens plus tard pour decouvrir de nouvelles personnes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasSearch) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onReset) {
                    Text("Effacer la recherche")
                }
            }
        }
    }
}

private fun rankUsersForSearch(users: List<User>, rawQuery: String): List<User> {
    val query = rawQuery.trim().lowercase()
    if (query.isBlank()) return users

    val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }
    return users
        .mapNotNull { user ->
            val username = user.username.lowercase()
            val bio = (user.bio ?: "").lowercase()
            val searchableText = "$username $bio"
            val matchesAllTerms = terms.all { searchableText.contains(it) }

            if (!matchesAllTerms) {
                null
            } else {
                RankedUser(
                    user = user,
                    exactMatch = username == query,
                    startsWithMatch = username.startsWith(query),
                    usernameMatches = terms.count { username.contains(it) },
                    bioMatches = terms.count { bio.contains(it) }
                )
            }
        }
        .sortedWith(
            compareByDescending<RankedUser> { it.exactMatch }
                .thenByDescending { it.startsWithMatch }
                .thenByDescending { it.usernameMatches }
                .thenByDescending { it.bioMatches }
                .thenByDescending { it.user.isOnline }
                .thenByDescending { it.user.followersCount }
                .thenBy { it.user.username.lowercase() }
        )
        .map { it.user }
}

private data class RankedUser(
    val user: User,
    val exactMatch: Boolean,
    val startsWithMatch: Boolean,
    val usernameMatches: Int,
    val bioMatches: Int
)
