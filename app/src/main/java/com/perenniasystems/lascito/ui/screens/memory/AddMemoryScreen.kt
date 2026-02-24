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
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    profileId:     String,
    onNavigateBack: () -> Unit = {}
) {
    var contextText by remember { mutableStateOf("") }
    val canSave = contextText.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "Nuevo Recuerdo",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint               = MutedGold
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
                    Icon(
                        imageVector        = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Seleccionar foto o video",
                        tint               = MutedGold,
                        modifier           = Modifier.size(56.dp)
                    )
                    Text(
                        text  = "Toca para elegir una foto o video",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
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
                        text  = "Escribe la historia detrás de este momento...",
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
                    focusedBorderColor   = MutedGoldDim,
                    unfocusedBorderColor = Color.Transparent,  // hoja de diario
                    cursorColor          = MutedGold,
                    focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                onClick  = {
                    // Phase 2: persist to Firestore / upload media to S3
                    onNavigateBack()
                },
                enabled  = canSave,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MutedGold,
                    contentColor           = AmoledBlack,
                    disabledContainerColor = MutedGoldDim,
                    disabledContentColor   = TextDisabled
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text  = "Preservar Recuerdo",
                    style = MaterialTheme.typography.labelLarge
                )
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
