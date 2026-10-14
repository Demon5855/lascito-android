package com.perenniasystems.lascito.ui.screens.memory

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.perenniasystems.lascito.ui.components.CustomAudioPlayer
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.components.MediaPreviewPlayer
import com.perenniasystems.lascito.ui.components.UploadProgressOverlay
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
import android.content.Intent
import android.provider.Settings
import com.perenniasystems.lascito.util.ImageCompressor
import com.perenniasystems.lascito.util.AudioRecorderHelper
import com.perenniasystems.lascito.data.model.MediaType
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

private const val MAX_CONTEXT_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    profileId:      String,
    viewModel:      AddMemoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
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
            if (started) {
                viewModel.updateSelectedMedia(null, MediaType.AUDIO)
                viewModel.setRecordingState(isRecording = true, duration = "00:00")
            }
        } else {
            onShowSnackbar(
                context.getString(R.string.error_mic_permission_denied),
                context.getString(R.string.action_settings)
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
    
    var recordingSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isActive) {
                delay(1000)
                recordingSeconds++
                val min = recordingSeconds / 60
                val sec = recordingSeconds % 60
                viewModel.setRecordingState(true, String.format(Locale.getDefault(), "%02d:%02d", min, sec))
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var isPickerLaunchEnabled by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is AddMemoryUiState.Loading
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    var showDeleteAudioWarning by remember { mutableStateOf(false) }

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
                        val titleText = if (uiState is AddMemoryUiState.Loaded) stringResource(R.string.screen_edit_memory) else stringResource(R.string.screen_new_memory)
                        Text(
                            text  = titleText,
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
            val currentMemory = (uiState as? AddMemoryUiState.Loaded)?.memory
            val cloudUrl = currentMemory?.mediaUrl
            
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
                    .clickable(enabled = isPickerLaunchEnabled && !isRecording && selectedMediaUri == null && cloudUrl.isNullOrBlank()) { 
                        coroutineScope.launch {
                            isPickerLaunchEnabled = false
                            showBottomSheet = true
                            delay(1000)
                            isPickerLaunchEnabled = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                
                val isAudioSetupMode = selectedMediaType == MediaType.AUDIO && selectedMediaUri == null && cloudUrl.isNullOrBlank()

                if (isAudioSetupMode || isRecording) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Text Timer at top
                        Text(
                            text = if (isRecording) recordingDuration else "00:00",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = PlayfairDisplay,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp)
                        )

                        // Studio Microphone
                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .scale(pulseScale)
                                        .alpha(pulseAlpha)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isRecording) {
                                        val uri = audioRecorder.stopRecording()
                                        viewModel.setRecordingState(isRecording = false)
                                        if (uri != null) {
                                            viewModel.updateSelectedMedia(uri, MediaType.AUDIO)
                                        }
                                    } else {
                                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Check else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Detener" else "Grabar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        // Trash button to cancel
                        if (isRecording || isAudioSetupMode) {
                            val showTrash = isRecording
                            if (showTrash) {
                                IconButton(
                                    onClick = {
                                        audioRecorder.cancelRecording()
                                        viewModel.setRecordingState(isRecording = false)
                                        viewModel.updateSelectedMedia(null, MediaType.IMAGE)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(24.dp)
                                        .size(48.dp)
                                        .background(SurfaceDark.copy(alpha=0.8f), CircleShape)
                                        .border(1.dp, CardBorder, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Cancelar",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedMediaUri != null || !cloudUrl.isNullOrBlank()) {
                    val urlToPlay = selectedMediaUri?.toString() ?: cloudUrl!!
                    when (selectedMediaType) {
                        MediaType.IMAGE -> {
                            AsyncImage(
                                model = selectedMediaUri ?: cloudUrl,
                                contentDescription = "Foto elegida",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        MediaType.AUDIO -> {
                            CustomAudioPlayer(url = urlToPlay)
                        }
                        MediaType.VIDEO -> {
                            MediaPreviewPlayer(uri = Uri.parse(urlToPlay), isAudioOnly = false)
                        }
                    }

                    // Botón "X" Global
                    IconButton(
                        onClick = {
                            if (selectedMediaType == MediaType.AUDIO) showDeleteAudioWarning = true else viewModel.clearMedia()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(AmoledBlack.copy(alpha=0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Quitar", tint = Color.White)
                    }
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
                onValueChange = { if (it.length <= MAX_CONTEXT_LENGTH) contextText = it },
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
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text(
                        text = "${contextText.length} / $MAX_CONTEXT_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is AddMemoryUiState.Error) {
                val errState = uiState as AddMemoryUiState.Error
                LascitoAlertDialog(
                    messageRes = errState.messageResId,
                    onDismiss = { viewModel.resetState() },
                    actionRes = if (errState.messageResId == R.string.error_email_not_verified) R.string.action_resend else null,
                    onAction = if (errState.messageResId == R.string.error_email_not_verified) {
                        { viewModel.resendVerificationEmail() }
                    } else null
                )
            }

            if (showDeleteAudioWarning) {
                val titleAudio = stringResource(R.string.dialog_delete_audio_title)
                val textAudio = stringResource(R.string.dialog_delete_audio_text)
                val textRemove = stringResource(R.string.action_remove)
                val textCancel = stringResource(R.string.btn_cancel)
                
                AlertDialog(
                    onDismissRequest = { showDeleteAudioWarning = false },
                    title = { Text(titleAudio) },
                    text = { Text(textAudio) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.clearMedia()
                            showDeleteAudioWarning = false
                        }) {
                            Text(textRemove, color = Color(0xFFFF5252))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAudioWarning = false }) {
                            Text(textCancel, color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary,
                    textContentColor = TextSecondary
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
                UploadProgressOverlay(
                    progress = uploadProgress,
                    message = stringResource(R.string.msg_uploading_memory)
                )
            }

            // -- Bottom Sheet Menu ---------------------------------------------
            if (showBottomSheet) {
                val labelPhoto = stringResource(R.string.action_add_photo)
                val labelVideo = stringResource(R.string.action_add_video)
                val labelAudio = stringResource(R.string.action_add_voice_note)
                
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
                            text = labelPhoto,
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
                            text = labelVideo,
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
                            text = labelAudio,
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    viewModel.updateSelectedMedia(null, MediaType.AUDIO)
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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

