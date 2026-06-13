package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Conversation
import com.example.data.models.User
import com.example.viewmodel.ChatViewModel
import com.example.ui.components.FullscreenStoryViewer
import com.example.ui.components.VerifiedBadge
import com.example.viewmodel.FeedViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    feedViewModel: FeedViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFindFriends: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Tous, 1 = Non lus, 2 = En ligne

    // Modal Sheet and Story Viewer states
    var showNewChatSheet by remember { mutableStateOf(false) }
    var activeStoryIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
        viewModel.loadContacts()
    }

    // Filtered lists
    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchQuery = conv.username.contains(searchQuery, ignoreCase = true) ||
                    (conv.lastMessage?.contains(searchQuery, ignoreCase = true) ?: false)
            
            val matchFilter = when (selectedFilter) {
                1 -> conv.unreadCount > 0
                2 -> conv.isOnline
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Title Header with fine typography and negative space
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Messagerie • STRIP",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onNavigateToFindFriends,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Trouver des amis", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showNewChatSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = "Nouvelle discussion", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Beautiful modern search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("chat_search_bar"),
                placeholder = { Text("Rechercher un contact, un message...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )

            // Dynamic filter pills matching design rules
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Tous", "Non lus", "En ligne")
                filters.forEachIndexed { idx, label ->
                    val isSelected = selectedFilter == idx
                    ElevatedAssistChip(
                        onClick = { selectedFilter = idx },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

            // Contact Stories Top Row
            if (stories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showNewChatSheet = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .border(BorderStroke(1.5.dp, Color.LightGray.copy(alpha = 0.3f)), shape = CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Ajouter story",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nouveau",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    itemsIndexed(stories) { index, story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeStoryIndex = index }
                                .padding(vertical = 4.dp)
                        ) {
                            val ringBrush = Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFFFF4081),
                                    Color(0xFFFFEB3B),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .border(BorderStroke(2.5.dp, ringBrush), shape = CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = story.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                    contentDescription = "Story avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = story.username,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(62.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            }

            // Conversations List Scroll
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Aucun message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune discussion trouvée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commencez une discussion en recherchant un ami ou en cliquant sur le bouton de chat.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredConversations) { conversation ->
                        Card(
                            onClick = { onNavigateToChat(conversation.userId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .testTag("conversation_card_${conversation.userId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (conversation.unreadCount > 0)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (conversation.unreadCount > 0)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else
                                    Color.LightGray.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with realistic online status indicator
                                Box {
                                    AsyncImage(
                                        model = conversation.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                    if (conversation.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color.White, CircleShape)
                                                .padding(2.dp)
                                                .align(Alignment.BottomEnd)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2ECC71)) // Fluent Green
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Message details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = conversation.username,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                            fontWeight = if (conversation.unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (conversation.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(size = 15.dp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = conversation.lastMessage ?: "Aucun message",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onBackground else Color.Gray,
                                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Meta status (badge/timestamp indicator)
                                if (conversation.unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = conversation.unreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // START NEW CHAT SHEET (Advanced Contact selection)
        if (showNewChatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNewChatSheet = false },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Nouvelle discussion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    Text(
                        text = "Sélectionnez un contact pour commencer à échanger avec style :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(contacts) { user ->
                            Card(
                                onClick = {
                                    showNewChatSheet = false
                                    onNavigateToChat(user.id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = user.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                            contentDescription = "Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                        )
                                        if (user.isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .padding(1.5.dp)
                                                    .align(Alignment.BottomEnd)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF2ECC71))
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(14.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.username,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (user.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VerifiedBadge(size = 14.dp)
                                            }
                                        }
                                        user.bio?.let {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FULLSCREEN STORY VIEWER (integrated in messagerie)
        if (activeStoryIndex in stories.indices) {
            FullscreenStoryViewer(
                stories = stories,
                initialIndex = activeStoryIndex,
                onAddComment = { id, content -> feedViewModel.addStoryComment(id, content) },
                onIncrementView = { id -> feedViewModel.incrementStoryView(id) },
                commentsProvider = { id -> feedViewModel.storyComments.collectAsState().value[id] ?: emptyList() },
                viewsProvider = { id -> feedViewModel.storyViews.collectAsState().value[id] ?: feedViewModel.getStoryViews(id) },
                onDismiss = { activeStoryIndex = -1 }
            )
        }
    }
}
