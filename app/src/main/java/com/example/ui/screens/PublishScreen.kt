package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.ActFileViewModel
import com.example.viewmodel.FeedViewModel
import androidx.navigation.NavController
import com.example.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    feedViewModel: FeedViewModel,
    actFileViewModel: ActFileViewModel? = null,
    navController: NavController? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by feedViewModel.isLoading.collectAsState()
    val selectedSound by feedViewModel.selectedSound.collectAsState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Vidéo, 1 = Story, 2 = ActFile

    DisposableEffect(Unit) {
        onDispose {
            feedViewModel.selectSound(null)
        }
    }
    
    // Video States
    var videoDescription by remember { mutableStateOf("") }
    var videoUrlInput by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Story States
    var selectedStoryUri by remember { mutableStateOf<Uri?>(null) }
    var storyUrlInput by remember { mutableStateOf("") }
    
    // ActFile States
    var actFileContent by remember { mutableStateOf("") }
    
    // Audio States
    var audioTitle by remember { mutableStateOf("Son original") }
    var audioOwner by remember { mutableStateOf("") }

    LaunchedEffect(selectedSound) {
        selectedSound?.let { sound ->
            audioTitle = sound.title
            audioOwner = sound.authorUsername
        }
    }
    
    // Effect States
    val effects = listOf(
        "Normal" to null,
        "Cyberpunk" to "cyberpunk",
        "Vintage" to "vintage",
        "Sketch" to "sketch",
        "Cartoon" to "cartoon",
        "Vignette" to "vignette"
    )
    var selectedEffect by remember { mutableStateOf<String?>(null) }
    
    // Picker Launchers
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audioTitle = "Son original"
            audioOwner = ""
            Toast.makeText(context, "Audio sélectionné (marqué comme Son original) !", Toast.LENGTH_SHORT).show()
        }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            videoUrlInput = "" // clear URL input if file is picked
            Toast.makeText(context, "Vidéo sélectionnée avec succès !", Toast.LENGTH_SHORT).show()
        }
    }
    
    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedStoryUri = uri
            storyUrlInput = "" // clear URL input if file is picked
            Toast.makeText(context, "Média sélectionné pour votre Story !", Toast.LENGTH_SHORT).show()
        }
    }
    
    val filterEffects = listOf(
        Pair("Normal", null),
        Pair("🔥 Cyberpunk", "cyberpunk"),
        Pair("✏️ Croquis", "sketch"),
        Pair("🎞️ Vintage", "vintage"),
        Pair("🎨 Cartoon", "cartoon"),
        Pair("💎 Vignette", "vignette"),
        Pair("🖤 Retro N&B", "retro_bw")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un Contenu") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Vidéo (TikTok)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Story (24h)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                if (actFileViewModel != null) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("ActFile (Txt)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content according to selected tab
            AnimatedContent(
                targetState = selectedTab,
                label = "publish_tabs",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { tab ->
                when (tab) {
                    0 -> { // Vidéo Tab
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Picker option from GALLERY
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selectedVideoUri != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (selectedVideoUri != null) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedVideoUri != null) {
                                    val filterColor = when(selectedEffect) {
                                        "cyberpunk" -> Color(0xFFFF00FF)
                                        "vintage" -> Color(0xFF8B4513)
                                        "retro_bw" -> Color.Gray
                                        "vignette" -> Color.Black
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val blendMode = when(selectedEffect) {
                                        "retro_bw" -> androidx.compose.ui.graphics.BlendMode.Saturation
                                        "cyberpunk" -> androidx.compose.ui.graphics.BlendMode.ColorDodge
                                        "vintage" -> androidx.compose.ui.graphics.BlendMode.Color
                                        else -> androidx.compose.ui.graphics.BlendMode.ColorBurn
                                    }

                                    AsyncImage(
                                        model = selectedVideoUri,
                                        contentDescription = "Aperçu de la vidéo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        colorFilter = if (selectedEffect != null) androidx.compose.ui.graphics.ColorFilter.tint(filterColor.copy(alpha = 0.5f), blendMode) else null
                                    )
                                    if (selectedEffect != null) {
                                        Text(
                                            text = "Effet: $selectedEffect",
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Choisir depuis ma Galerie",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Vitesse d'upload maximale en Cloudinary",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Effect picker
                            Text(
                                text = "Appliquer un filtre ou effet visuel (Cloudinary) :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filterEffects) { effect ->
                                    FilterChip(
                                        selected = selectedEffect == effect.second,
                                        onClick = { selectedEffect = effect.second },
                                        label = { Text(effect.first) }
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            OutlinedTextField(
                                value = videoDescription,
                                onValueChange = { videoDescription = it },
                                label = { Text("Légende / Description") },
                                placeholder = { Text("Quoi de neuf ? #tendance #strip") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 5,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                            
                            if (selectedVideoUri == null) {
                                Text(
                                    text = "OU Saisir l'adresse URL d'une vidéo :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = videoUrlInput,
                                    onValueChange = { 
                                        videoUrlInput = it
                                        if (it.isNotEmpty()) selectedVideoUri = null 
                                    },
                                    label = { Text("URL de la Vidéo") },
                                    placeholder = { Text("https://example.com/video.mp4") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 1,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                                )
                            }

                            // Audio Selection Section
                            Text(
                                text = "Paramètres Audio :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = audioTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    if (audioOwner.isNotBlank()) {
                                        Text(text = "par $audioOwner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (navController != null) {
                                    IconButton(onClick = {
                                        navController.navigate(Routes.SOUND_LIBRARY)
                                    }) {
                                        Icon(Icons.Default.Explore, contentDescription = "Sons Viraux", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = {
                                    audioPickerLauncher.launch("audio/*")
                                }) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = "Pick Audio", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Suggestions of existing audio (simulated from current videos)
                            val feedVideos by feedViewModel.videos.collectAsState()
                            if (feedVideos.isNotEmpty()) {
                                Text(text = "Utiliser un son existant :", style = MaterialTheme.typography.labelSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(feedVideos.take(5)) { v ->
                                        AssistChip(
                                            onClick = {
                                                audioTitle = v.audioTitle ?: "Son original"
                                                audioOwner = v.username
                                            },
                                            label = { Text(v.audioTitle ?: v.username) },
                                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }
                            }

                            // Effects Selection
                            Text(
                                text = "Appliquer un Effet :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(effects) { (name, id) ->
                                    FilterChip(
                                        selected = selectedEffect == id,
                                        onClick = { selectedEffect = id },
                                        label = { Text(name) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // Story Tab
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Picker option from GALLERY
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selectedStoryUri != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (selectedStoryUri != null) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        storyPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedStoryUri != null) {
                                    val filterColor = when(selectedEffect) {
                                        "cyberpunk" -> Color(0xFFFF00FF)
                                        "vintage" -> Color(0xFF8B4513)
                                        "retro_bw" -> Color.Gray
                                        "vignette" -> Color.Black
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val blendMode = when(selectedEffect) {
                                        "retro_bw" -> androidx.compose.ui.graphics.BlendMode.Saturation
                                        "cyberpunk" -> androidx.compose.ui.graphics.BlendMode.ColorDodge
                                        "vintage" -> androidx.compose.ui.graphics.BlendMode.Color
                                        else -> androidx.compose.ui.graphics.BlendMode.ColorBurn
                                    }

                                    AsyncImage(
                                        model = selectedStoryUri,
                                        contentDescription = "Aperçu de la story",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        colorFilter = if (selectedEffect != null) androidx.compose.ui.graphics.ColorFilter.tint(filterColor.copy(alpha = 0.5f), blendMode) else null
                                    )
                                    if (selectedEffect != null) {
                                        Text(
                                            text = "Filtre: $selectedEffect",
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Photos et vidéos de ma Galerie",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Sélection individuelle sécurisée",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Advanced Interactive Filter Effects selection (OpenCV supported)
                            Text(
                                text = "Appliquer un filtre ou effet visuel :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filterEffects) { effect ->
                                    FilterChip(
                                        selected = selectedEffect == effect.second,
                                        onClick = { selectedEffect = effect.second },
                                        label = { Text(effect.first) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            if (selectedStoryUri == null) {
                                Text(
                                    text = "OU Saisir l'adresse URL d'un média :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = storyUrlInput,
                                    onValueChange = { 
                                        storyUrlInput = it
                                        if (it.isNotEmpty()) selectedStoryUri = null
                                    },
                                    label = { Text("URL de la Story") },
                                    placeholder = { Text("https://example.com/story.jpg") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 1,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                                )
                            }
                        }
                    }
                    2 -> { // ActFile Tab
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = actFileContent,
                                onValueChange = { actFileContent = it },
                                label = { Text("Contenu du message (Markdown accepté)") },
                                placeholder = { Text("Donnez libre cours à votre créature ici... #markdown") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Default)
                                // We don't put 'Done' here usually because they might want to enter multiple lines with the return key
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Vous pouvez utiliser du gras, de l'italique et des hashtags.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Primary Action Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when (selectedTab) {
                        0 -> { // Video Publish
                            if (selectedVideoUri != null) {
                                feedViewModel.uploadAndPublishVideo(
                                    context, 
                                    selectedVideoUri!!, 
                                    videoDescription.ifBlank { "Nouvelle vidéo 📹" },
                                    audioTitle,
                                    audioOwner,
                                    selectedEffect
                                ) { success, errorMsg ->
                                    if (success) {
                                        Toast.makeText(context, "Vidéo publiée avec succès !", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Erreur lors de l'upload : $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else if (videoUrlInput.isNotBlank()) {
                                feedViewModel.publishVideo(
                                    videoDescription.ifBlank { "Preset vidéo !" }, 
                                    videoUrlInput, 
                                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=400&q=80"
                                )
                                Toast.makeText(context, "Vidéo publiée avec succès !", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Veuillez sélectionner ou saisir une vidéo", Toast.LENGTH_LONG).show()
                            }
                        }
                        1 -> { // Story Publish
                            if (selectedStoryUri != null) {
                                feedViewModel.uploadAndPublishStory(
                                    context, 
                                    selectedStoryUri!!, 
                                    selectedEffect
                                ) { success, errorMsg ->
                                    if (success) {
                                        Toast.makeText(context, "Story ajoutée et traitée avec succès !", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Erreur lors de l'upload story : $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else if (storyUrlInput.isNotBlank()) {
                                feedViewModel.publishStory(storyUrlInput)
                                Toast.makeText(context, "Story ajoutée avec succès !", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Veuillez sélectionner ou renseigner un média", Toast.LENGTH_LONG).show()
                            }
                        }
                        2 -> { // ActFile Publish
                            if (actFileContent.isNotBlank() && actFileViewModel != null) {
                                actFileViewModel.createActFile(actFileContent)
                                Toast.makeText(context, "Publication créée avec succès !", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Le contenu de la publication ne peut pas être vide", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isLoading && (
                    (selectedTab == 0 && (selectedVideoUri != null || videoUrlInput.isNotBlank())) ||
                    (selectedTab == 1 && (selectedStoryUri != null || storyUrlInput.isNotBlank())) ||
                    (selectedTab == 2 && actFileContent.isNotBlank())
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Traitement & Upload en cours...", fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Text(
                            text = when (selectedTab) {
                                0 -> "Publier la Vidéo"
                                1 -> "Partager la Story"
                                2 -> "Diffuser sur ActFiles"
                                else -> "Publier"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
