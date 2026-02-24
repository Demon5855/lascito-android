package com.perenniasystems.lascito.ui.screens.altar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.theme.*
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
    viewModel:      AltarViewModel = viewModel()
) {
    // Load data when the screen enters composition
    LaunchedEffect(profileId) { viewModel.load(profileId) }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AmoledBlack,
        floatingActionButton = {
            if (uiState is AltarUiState.Success) {
                FloatingActionButton(
                    onClick        = onAddMemory,
                    containerColor = MutedGold,
                    contentColor   = AmoledBlack,
                    shape          = CircleShape,
                    elevation      = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Añadir recuerdo"
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
                        color    = MutedGold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AltarUiState.Error -> {
                    Text(
                        text      = state.message,
                        style     = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                is AltarUiState.Success -> {
                    AltarContent(
                        profile       = state.profile,
                        memories      = state.memories,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun AltarContent(
    profile:       Profile,
    memories:      List<Memory>,
    onNavigateBack: () -> Unit
) {
    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val birthYear    = yearFmt.format(Date(profile.birthDate))
    val passingLabel = profile.passingDate?.let { yearFmt.format(Date(it)) } ?: "Presente"
    val dateRange    = "$birthYear — $passingLabel"

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(bottom = 96.dp)
    ) {
        // ── HERO SECTION ──────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                // Avatar a pantalla completa
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = profile.fullName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )

                // Placeholder sin imagen
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = if (profile.profileType == ProfileType.PET) "🐾" else "✦",
                        style = MaterialTheme.typography.displayLarge.copy(color = MutedGoldDim)
                    )
                }

                // Degradado inferior imagen → fondo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, AmoledBlack)
                            )
                        )
                )

                // Botón atrás flotante sobre la imagen
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

        // ── CABECERA DE INFORMACIÓN ───────────────────────────────────────────
        item {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-32).dp),   // sube sobre el degradado
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text      = profile.fullName,
                    style     = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = PlayfairDisplay
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "${profile.relationship}  ·  $dateRange",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MutedGoldDim),
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

        // ── SECCIÓN TÍTULO "Memorias" ─────────────────────────────────────────
        item {
            Text(
                text     = "Memorias",
                style    = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = PlayfairDisplay
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── MURO DE MEMORIAS ─────────────────────────────────────────────────
        if (memories.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "Aún no hay recuerdos.\nPresiona + para añadir el primero.",
                        style     = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(memories, key = { it.memoryId }) { memory ->
                MemoryCard(memory = memory, dateFmt = dateFmt)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MemoryCard(
    memory:  Memory,
    dateFmt: SimpleDateFormat
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Imagen del recuerdo — ancho completo
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(memory.mediaUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        // Texto contextual y fecha
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
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
                style = MaterialTheme.typography.bodySmall.copy(color = MutedGoldDim)
            )
        }

        HorizontalDivider(
            color     = CardBorder,
            thickness = 0.5.dp,
            modifier  = Modifier.padding(horizontal = 24.dp)
        )
    }
}
