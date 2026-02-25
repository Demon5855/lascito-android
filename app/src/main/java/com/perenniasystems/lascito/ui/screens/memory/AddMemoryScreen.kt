package com.perenniasystems.lascito.ui.screens.memory

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.viewmodel.AddMemoryUiState
import com.perenniasystems.lascito.viewmodel.AddMemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    profileId:      String,
    viewModel:      AddMemoryViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var contextText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AddMemoryUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AddMemoryUiState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    val canSave = contextText.isNotBlank() && !isLoading

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Media Picker Area ─────────────────────────────────────────────
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
                    .clickable { /* Phase 2: Launch photo/video picker */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tres íconos centrados horizontalmente
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

            // ── Diary-style Text Field ────────────────────────────────────────
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
                Text(
                    text     = (uiState as AddMemoryUiState.Error).message,
                    color    = ErrorRed,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )
            }

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                onClick  = {
                    viewModel.saveMemory(profileId = profileId, contextText = contextText)
                },
                enabled  = canSave,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = AmoledBlack,
                    disabledContainerColor = SurfaceVariant,   // gray — clearly blocked
                    disabledContentColor   = TextDisabled
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AmoledBlack,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text  = stringResource(R.string.btn_preserve_memory),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AddMemoryScreenPreview() {
    LascitoTheme { AddMemoryScreen(profileId = "prof-001") }
}
