package com.perenniasystems.lascito.ui.screens.altar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.components.shimmerEffect
import com.perenniasystems.lascito.viewmodel.AltarUiState
import com.perenniasystems.lascito.viewmodel.AltarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltarScreen(
    profileId:      String,
    onNavigateBack: () -> Unit = {},
    onAddMemory:    () -> Unit = {},
    onEditMemory:   (String) -> Unit = {},
    onDeleteMemory: (String) -> Unit = {},
    viewModel:      AltarViewModel = viewModel()
) {
    // Trigger reload when the profileId changes OR when the screen becomes active again
    LaunchedEffect(profileId, Unit) { viewModel.load(profileId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }

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
                        onEditMemory   = onEditMemory,
                        onDeleteMemory = { memoryId ->
                            memoryToDelete = state.memories.find { it.memoryId == memoryId }
                        }
                    )
                }
            }

            if (memoryToDelete != null) {
                AlertDialog(
                    onDismissRequest = { memoryToDelete = null },
                    containerColor = SurfaceDark,
                    titleContentColor = ErrorRed,
                    textContentColor = TextPrimary,
                    title = { Text(stringResource(R.string.confirm_delete_memory_title)) },
                    text = { Text(stringResource(R.string.confirm_delete_memory_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                memoryToDelete?.let {
                                    viewModel.deleteMemory(it.memoryId, profileId, it.mediaUrl)
                                }
                                memoryToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.action_delete), color = ErrorRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { memoryToDelete = null }) {
                            Text(stringResource(R.string.btn_cancel), color = TextSecondary)
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
    onEditMemory:   (String) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val birthYear    = yearFmt.format(Date(profile.birthDate))
    val passingLabel = profile.passingDate?.let { yearFmt.format(Date(it)) } ?: "Presente"
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
                        contentDescription = "Volver",
                        tint               = TextPrimary
                    )
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
                    text      = "${profile.relationship}  •  $dateRange",
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
            // Muestra el thumbnail con un overlay oscuro y un ícono Play centrado
            MediaType.VIDEO -> {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Thumbnail
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(memory.mediaUrl.takeIf { it.isNotBlank() })
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
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
                                Icon(
                                    imageVector        = Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primaryContainer,
                                    modifier           = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                    // Oscurecimiento sutil
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000))
                    )
                    // Botón Play dorado
                    Icon(
                        imageVector        = Icons.Default.PlayCircle,
                        contentDescription = "Reproducir video",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(72.dp)
                    )
                }
            }

            // -- AUDIO ---------------------------------------------------------
            // Reproductor minimalista: play + barra de progreso + tiempo
            MediaType.AUDIO -> {
                AudioPlayer()
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = memory.contextText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color      = TextPrimary,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text  = dateFmt.format(Date(memory.capturedDate)),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primaryContainer)
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit), color = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = ErrorRed) },
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
// AudioPlayer — reproductor estático minimalista (Phase 1: visual sólo)
// ----------------------------------------------------------------------------

@Composable
private fun AudioPlayer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Botón Play circular dorado
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.PlayCircle,
                contentDescription = "Reproducir nota de voz",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(32.dp)
            )
        }

        // Barra de progreso + tiempo
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LinearProgressIndicator(
                progress            = { 0.35f },   // Phase 2: progreso real del player
                modifier            = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color               = MaterialTheme.colorScheme.primary,
                trackColor          = CardBorder,
                strokeCap           = StrokeCap.Round
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "0:31",    // Phase 2: posición real
                    style = MaterialTheme.typography.bodySmall.copy(
                        color    = TextSecondary,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text  = "1:24",    // Phase 2: duración real
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
