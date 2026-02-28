package com.perenniasystems.lascito.ui.screens.altar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.components.CustomAudioPlayer
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.components.shimmerEffect
import com.perenniasystems.lascito.viewmodel.AltarUiState
import com.perenniasystems.lascito.viewmodel.AltarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltarScreen(
    profileId:      String,
    onNavigateBack: () -> Unit = {},
    onAddMemory:    () -> Unit = {},
    onEditMemory:   (String) -> Unit = {},
    onDeleteMemory: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onDeleteProfileClick: () -> Unit = {},
    viewModel:      AltarViewModel = viewModel()
) {
    // Trigger reload when the profileId changes OR when the screen becomes active again
    LaunchedEffect(profileId, Unit) { viewModel.load(profileId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }
    var showDeleteProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AmoledBlack,
        floatingActionButton = {
            if (uiState is AltarUiState.Success) {
                FloatingActionButton(
                    onClick        = onAddMemory,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = AmoledBlack,
                    shape          = CircleShape,
                    elevation      = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.desc_add_memory)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AltarUiState.Loading -> {
                    CircularProgressIndicator(
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is AltarUiState.Error -> {
                    LascitoAlertDialog(
                        messageRes = state.messageResId,
                        onDismiss  = onNavigateBack
                    )
                }
                is AltarUiState.Success -> {
                    AltarContent(
                        profile        = state.profile,
                        memories       = state.memories,
                        onNavigateBack = onNavigateBack,
                        onEditProfileClick = onEditProfileClick,
                        onDeleteProfileClick = { showDeleteProfileDialog = true },
                        onEditMemory   = onEditMemory,
                        onDeleteMemory = { memoryId ->
                            memoryToDelete = state.memories.find { it.memoryId == memoryId }
                        }
                    )
                }
            }

            if (memoryToDelete != null) {
                val titleMem = stringResource(R.string.confirm_delete_memory_title)
                val textMem = stringResource(R.string.confirm_delete_memory_text)
                val btnDeleteMem = stringResource(R.string.action_delete)
                val btnCancelMem = stringResource(R.string.btn_cancel)
                
                AlertDialog(
                    onDismissRequest = { memoryToDelete = null },
                    containerColor = SurfaceDark,
                    titleContentColor = ErrorRed,
                    textContentColor = TextPrimary,
                    title = { Text(titleMem) },
                    text = { Text(textMem) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                memoryToDelete?.let {
                                    viewModel.deleteMemory(it.memoryId, profileId, it.mediaUrl)
                                }
                                memoryToDelete = null
                            }
                        ) {
                            Text(btnDeleteMem, color = ErrorRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { memoryToDelete = null }) {
                            Text(btnCancelMem, color = TextSecondary)
                        }
                    }
                )
            }

            if (showDeleteProfileDialog) {
                val profile = (uiState as? AltarUiState.Success)?.profile
                if (profile == null) {
                    Box(modifier = Modifier.fillMaxSize())
                    return@Scaffold
                }
                val titleProf = stringResource(R.string.confirm_delete_profile_title)
                val textProf = stringResource(R.string.confirm_delete_profile_text, profile?.fullName ?: "")
                val btnDeleteProf = stringResource(R.string.action_delete)
                val btnCancelProf = stringResource(R.string.btn_cancel)
                
                AlertDialog(
                    onDismissRequest = { showDeleteProfileDialog = false },
                    containerColor = SurfaceDark,
                    titleContentColor = ErrorRed,
                    textContentColor = TextPrimary,
                    title = { Text(titleProf) },
                    text = { Text(textProf) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                profile?.let {
                                    viewModel.deleteProfile(it.profileId, it.avatarUrl, onSuccess = {
                                        onDeleteProfileClick()
                                    })
                                }
                                showDeleteProfileDialog = false
                            }
                        ) {
                            Text(btnDeleteProf, color = ErrorRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteProfileDialog = false }) {
                            Text(btnCancelProf, color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AltarContent(
    profile:       Profile,
    memories:      List<Memory>,
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onDeleteProfileClick: () -> Unit,
    onEditMemory:   (String) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    val textRefineProfile = stringResource(R.string.action_edit)
    val textDeleteProfile = stringResource(R.string.action_delete)

    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val birthYear    = yearFmt.format(Date(profile.birthDate))
    val passingLabel = profile.passingDate?.let { yearFmt.format(Date(it)) } ?: stringResource(R.string.label_present)
    val dateRange    = "$birthYear — $passingLabel"

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // -- HERO -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                // Foto del perfil (con Loading Shimmer y Error state combinados)
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile.avatarUrl.takeIf { it.isNotBlank() })
                        .crossfade(true)
                        .build(),
                    contentDescription = profile.fullName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    },
                    error = {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.profileType == ProfileType.PET) {
                                Text(
                                    text  = "🐾",
                                    style = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.primaryContainer)
                                )
                            } else {
                                Icon(
                                    imageVector        = Icons.Default.Person,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primaryContainer,
                                    modifier           = Modifier.size(72.dp)
                                )
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, AmoledBlack)))
                )

                // -- Badge tipo de perfil (esquina inferior derecha) -----------
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(40.dp)
                        .background(AmoledBlack.copy(alpha = 0.72f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profileType == ProfileType.PET) {
                        Text(text = "🐾", fontSize = 18.sp)
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Person,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick  = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(AmoledBlack.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.desc_back),
                        tint               = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .wrapContentSize(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = { showProfileMenu = true },
                        modifier = Modifier.background(AmoledBlack.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.desc_options),
                            tint               = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text(textRefineProfile, color = TextPrimary) },
                            onClick = {
                                showProfileMenu = false
                                onEditProfileClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(textDeleteProfile, color = ErrorRed) },
                            onClick = {
                                showProfileMenu = false
                                onDeleteProfileClick()
                            }
                        )
                    }
                }
            }
        }

        // -- CABECERA INFO -----------------------------------------------------
        item {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-32).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text      = profile.fullName,
                    style     = MaterialTheme.typography.displayMedium.copy(fontFamily = PlayfairDisplay),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text      = "${profile.relationship}\n$dateRange",
                    style     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primaryContainer),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    color     = CardBorder,
                    thickness = 0.5.dp,
                    modifier  = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // -- TÍTULO ------------------------------------------------------------
        item {
            Text(
                text     = stringResource(R.string.screen_memories),
                style    = MaterialTheme.typography.headlineLarge.copy(fontFamily = PlayfairDisplay),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // -- MURO DE MEMORIAS --------------------------------------------------
        if (memories.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = stringResource(R.string.empty_memories),
                        style     = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(memories, key = { it.memoryId }) { memory ->
                MemoryItem(
                    memory = memory, 
                    dateFmt = dateFmt,
                    onEdit = { onEditMemory(memory.memoryId) },
                    onDelete = { onDeleteMemory(memory.memoryId) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------------
// MemoryItem — dispatcher por tipo de media
// ----------------------------------------------------------------------------

@Composable
private fun MemoryItem(
    memory: Memory, 
    dateFmt: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        when (memory.mediaType) {

            // -- IMAGE ---------------------------------------------------------
            MediaType.IMAGE -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(memory.mediaUrl.takeIf { it.isNotBlank() })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    },
                    error = {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.ImageNotSupported,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primaryContainer,
                                modifier           = Modifier.size(48.dp)
                            )
                        }
                    }
                )
            }

            // -- VIDEO ---------------------------------------------------------
            MediaType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    FeedMediaItem(url = memory.mediaUrl, isAudioOnly = false)
                }
            }

            // -- AUDIO ---------------------------------------------------------
            MediaType.AUDIO -> {
                CustomAudioPlayer(url = memory.mediaUrl)
            }
        }

        // -- Contexto y fecha (comunes a todos los tipos) ----------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).animateContentSize()) {
                var isExpanded by remember { mutableStateOf(false) }
                var showSeeMore by remember { mutableStateOf(false) }
                
                Text(
                    text  = memory.contextText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color      = TextPrimary,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.hasVisualOverflow) {
                            showSeeMore = true
                        }
                    }
                )
                
                if (showSeeMore || isExpanded) {
                    Text(
                        text = if (isExpanded) stringResource(R.string.action_see_less) else stringResource(R.string.action_see_more),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { isExpanded = !isExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text  = dateFmt.format(Date(memory.capturedDate)),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primaryContainer)
                )
            }
            
            var showMenu by remember { mutableStateOf(false) }
            val textEditMem = stringResource(R.string.action_edit)
            val textDeleteMem = stringResource(R.string.action_delete)

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.desc_options),
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text(textEditMem, color = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(textDeleteMem, color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        HorizontalDivider(
            color     = CardBorder,
            thickness = 0.5.dp,
            modifier  = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// FeedMediaItem — Dynamic AV renderer with safe ExoPlayer lifting
// ----------------------------------------------------------------------------
@Composable
private fun FeedMediaItem(url: String, isAudioOnly: Boolean) {
    if (url.isBlank()) return

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                if (isAudioOnly) {
                    controllerAutoShow = true
                    controllerShowTimeoutMs = -1
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                } else {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


