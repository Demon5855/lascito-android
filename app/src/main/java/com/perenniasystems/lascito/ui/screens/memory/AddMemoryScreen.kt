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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.perenniasystems.lascito.util.ImageCompressor
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
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedImageUri = uri }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPickerLaunchEnabled by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is AddMemoryUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AddMemoryUiState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    val canSave = contextText.isNotBlank() && selectedImageUri != null && !isLoading

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
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Foto elegida",
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
            Button(
                onClick  = {
                    coroutineScope.launch {
                        val bytes = selectedImageUri?.let { ImageCompressor.compressImage(context, it) }
                        viewModel.saveMemory(
                            profileId = profileId,
                            contextText = contextText,
                            imageBytes = bytes
                        )
                    }
                },
                enabled  = canSave,
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
                            text = "Añadir Fotografía",
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
                            text = "Añadir Video",
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    android.widget.Toast.makeText(context, "Video: Próximamente (Fase 3B)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        MediaBottomSheetItem(
                            icon = Icons.Default.Mic,
                            text = "Añadir Nota de Voz",
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    android.widget.Toast.makeText(context, "Audio: Próximamente (Fase 3B)", android.widget.Toast.LENGTH_SHORT).show()
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

