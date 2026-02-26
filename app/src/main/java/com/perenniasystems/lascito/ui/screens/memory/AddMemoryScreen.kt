package com.perenniasystems.lascito.ui.screens.memory

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.viewmodel.AddMemoryUiState
import com.perenniasystems.lascito.viewmodel.AddMemoryViewModel

import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.perenniasystems.lascito.util.ImageCompressor
import com.perenniasystems.lascito.util.AudioRecorderHelper
import com.perenniasystems.lascito.data.model.MediaType
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    profileId:      String,
    viewModel:      AddMemoryViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var contextText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val selectedMediaUri by viewModel.selectedMediaUri.collectAsStateWithLifecycle()
    val selectedMediaType by viewModel.selectedMediaType.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    
    val audioRecorder = remember { AudioRecorderHelper(context) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.updateSelectedMedia(uri, MediaType.IMAGE) }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.updateSelectedMedia(uri, MediaType.VIDEO) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = audioRecorder.startRecording()
            if (started) viewModel.setRecordingState(isRecording = true, duration = "Grabando...")
        } else {
            android.widget.Toast.makeText(context, "Permiso de micrófono denegado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var isPickerLaunchEnabled by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is AddMemoryUiState.Loading

    // Handle initial load and success navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddMemoryUiState.Success -> {
                viewModel.resetState()
                onNavigateBack()
            }
            is AddMemoryUiState.Loaded -> {
                val memory = (uiState as AddMemoryUiState.Loaded).memory
                contextText = memory.contextText
            }
            else -> {}
        }
    }

    val canSave = contextText.isNotBlank() && selectedMediaUri != null && !isLoading && !isRecording

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = stringResource(R.string.screen_new_memory),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors  = TopAppBarDefaults.topAppBarColors(
                    containerColor    = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = AmoledBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

            // -- Media Picker Area ---------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(
                        width = 1.dp,
                        color = CardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = isPickerLaunchEnabled) { 
                        coroutineScope.launch {
                            isPickerLaunchEnabled = false
                            showBottomSheet = true
                            delay(1000)
                            isPickerLaunchEnabled = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentMemory = (uiState as? AddMemoryUiState.Loaded)?.memory
                
                if (isRecording) {
                    Column(
                        modifier = Modifier.fillMaxSize().clickable {
                            val uri = audioRecorder.stopRecording()
                            viewModel.setRecordingState(isRecording = false)
                            if (uri != null) {
                                viewModel.updateSelectedMedia(uri, MediaType.AUDIO)
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Grabando",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = recordingDuration,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca para detener y guardar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else if (selectedMediaUri != null) {
                    when (selectedMediaType) {
                        MediaType.IMAGE -> {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Foto elegida",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        MediaType.VIDEO, MediaType.AUDIO -> {
                            MediaPreviewPlayer(uri = selectedMediaUri!!, isAudioOnly = selectedMediaType == MediaType.AUDIO)
                        }
                    }
                } else if (currentMemory != null && currentMemory.mediaUrl.isNotBlank()) {
                     AsyncImage( // For simplicity right now, treating old memories as images, can update later
                        model = currentMemory.mediaUrl,
                        contentDescription = "Media actual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tres iconos centrados horizontalmente
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment     = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.PhotoCamera,
                                contentDescription = stringResource(R.string.desc_photo),
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(32.dp)
                            )
                            Icon(
                                imageVector        = Icons.Default.Videocam,
                                contentDescription = stringResource(R.string.desc_video),
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(32.dp)
                            )
                            Icon(
                                imageVector        = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.desc_voice_note),
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text  = stringResource(R.string.hint_pick_media),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // -- Diary-style Text Field ----------------------------------------
            OutlinedTextField(
                value         = contextText,
                onValueChange = { contextText = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                placeholder   = {
                    Text(
                        text  = stringResource(R.string.hint_context_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color     = TextDisabled,
                            fontStyle = FontStyle.Italic
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color      = TextPrimary,
                    lineHeight = 28.sp
                ),
                minLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors   = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = Color.Transparent,  // hoja de diario
                    cursorColor          = MaterialTheme.colorScheme.primary,
                    focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is AddMemoryUiState.Error) {
                val errState = uiState as AddMemoryUiState.Error
                LascitoAlertDialog(
                    messageRes = errState.messageResId,
                    onDismiss  = { viewModel.resetState() }
                )
            }
            // -- Save Button ---------------------------------------------------
            val canSaveBtn = contextText.isNotBlank() && (selectedMediaUri != null || (uiState as? AddMemoryUiState.Loaded)?.memory?.mediaUrl?.isNotBlank() == true) && !isLoading && !isRecording
            Button(
                onClick  = {
                    coroutineScope.launch {
                        val bytes = if (selectedMediaType == MediaType.IMAGE && selectedMediaUri != null) {
                             ImageCompressor.compressImage(context, selectedMediaUri!!)
                        } else null
                        
                        viewModel.saveMemory(
                            profileId = profileId,
                            contextText = contextText,
                            mediaType = selectedMediaType,
                            imageBytes = bytes,
                            mediaUri = selectedMediaUri
                        )
                    }
                },
                enabled  = canSaveBtn,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = AmoledBlack,
                    disabledContainerColor = SurfaceVariant,   // gray � clearly blocked
                    disabledContentColor   = TextDisabled
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text  = stringResource(R.string.btn_preserve_memory),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            } // End of Column

            // -- Loading Overlay ---------------------------------------------------
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledBlack.copy(alpha = 0.6f))
                        .clickable(enabled = false) { /* Block clicks */ },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }

            // -- Bottom Sheet Menu ---------------------------------------------
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, top = 8.dp)
                    ) {
                        MediaBottomSheetItem(
                            icon = Icons.Default.PhotoCamera,
                            text = stringResource(R.string.action_add_photo),
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            }
                        )
                        MediaBottomSheetItem(
                            icon = Icons.Default.Videocam,
                            text = stringResource(R.string.action_add_video),
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    videoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            }
                        )
                        MediaBottomSheetItem(
                            icon = Icons.Default.Mic,
                            text = stringResource(R.string.action_add_voice_note),
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                }
            }
        } // End of outer Box
    } // End of Scaffold Container
}

// -- Helpers -------------------------------------------------------------------

@Composable
private fun MediaPreviewPlayer(uri: Uri, isAudioOnly: Boolean = false) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
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
                    // Hide UI that implies video if it's strictly audio
                    controllerAutoShow = true
                    controllerShowTimeoutMs = -1 // Don't hide the controls
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MediaBottomSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, // Dorado
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary) // Gris claro / Blanco
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AddMemoryScreenPreview() {
    LascitoTheme { AddMemoryScreen(profileId = "prof-001") }
}

