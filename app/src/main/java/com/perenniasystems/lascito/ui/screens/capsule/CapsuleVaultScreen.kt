package com.perenniasystems.lascito.ui.screens.capsule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.TimeCapsule
import com.perenniasystems.lascito.data.model.TriggerType
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.viewmodel.CapsuleVaultViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleVaultScreen(
    viewModel:           CapsuleVaultViewModel = viewModel(),
    onNavigateBack:      () -> Unit            = {},
    onNavigateToCreate:  () -> Unit            = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cápsula pendiente de confirmación de borrado
    var pendingDelete by remember { mutableStateOf<TimeCapsule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = stringResource(R.string.screen_capsule_vault),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = { Spacer(Modifier.width(48.dp)) },
                colors  = TopAppBarDefaults.topAppBarColors(
                    containerColor    = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onNavigateToCreate,
                containerColor   = MaterialTheme.colorScheme.primary,
                contentColor     = AmoledBlack,
                shape            = CircleShape,
                modifier         = Modifier.size(60.dp)
            ) {
                Icon(
                    Icons.Default.Create,
                    contentDescription = stringResource(R.string.btn_new_capsule_fab),
                    modifier           = Modifier.size(26.dp)
                )
            }
        },
        containerColor = AmoledBlack
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center).size(48.dp),
                        strokeWidth = 3.dp,
                        color       = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.capsules.isEmpty() -> {
                    // ── Empty State ────────────────────────────────────────────
                    Column(
                        modifier            = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text      = stringResource(R.string.vault_empty_title),
                            style     = MaterialTheme.typography.titleMedium.copy(
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlayfairDisplay
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text      = stringResource(R.string.vault_empty_subtitle),
                            style     = MaterialTheme.typography.bodyMedium.copy(
                                color      = TextSecondary,
                                lineHeight = 22.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                else -> {
                    // ── Lista de Cápsulas ──────────────────────────────────────
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(
                            start  = 20.dp,
                            end    = 20.dp,
                            top    = 16.dp,
                            bottom = 88.dp   // espacio para el FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text  = stringResource(R.string.vault_capsule_count, uiState.capsules.size),
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(
                            items = uiState.capsules,
                            key   = { it.capsuleId }
                        ) { capsule ->
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(),
                                exit    = fadeOut()
                            ) {
                                CapsuleItemCard(
                                    capsule   = capsule,
                                    onDelete  = { pendingDelete = capsule }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── AlertDialog de error ───────────────────────────────────────────────────
    uiState.error?.let {
        AlertDialog(
            onDismissRequest  = { viewModel.clearError() },
            containerColor    = SurfaceDark,
            titleContentColor = ErrorRed,
            textContentColor  = TextPrimary,
            title = { Text(stringResource(R.string.error_capsule_save_title)) },
            text  = { Text(stringResource(R.string.error_guardian_save_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(R.string.btn_understood), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // ── AlertDialog de confirmación de borrado ─────────────────────────────────
    pendingDelete?.let { capsule ->
        AlertDialog(
            onDismissRequest  = { pendingDelete = null },
            containerColor    = SurfaceDark,
            titleContentColor = ErrorRed,
            textContentColor  = TextPrimary,
            icon = {
                Icon(Icons.Default.DeleteForever, null,
                    tint     = ErrorRed,
                    modifier = Modifier.size(28.dp))
            },
            title = { Text(stringResource(R.string.dialog_delete_capsule_title)) },
            text  = {
                Text(
                    stringResource(R.string.dialog_delete_capsule_body, capsule.title),
                    style = LocalTextStyle.current.copy(lineHeight = 20.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCapsule(capsule)
                    pendingDelete = null
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color      = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CapsuleItemCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CapsuleItemCard(
    capsule:  TimeCapsule,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Header: Título + Botón borrar ──────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = capsule.title.ifBlank { stringResource(R.string.capsule_untitled) },
                    style      = MaterialTheme.typography.titleSmall.copy(
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlayfairDisplay
                    ),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f).padding(end = 8.dp)
                )
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint               = TextSecondary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            // ── Body: Preview del contenido ────────────────────────────────────
            if (!capsule.content.isNullOrBlank()) {
                Text(
                    text     = capsule.content,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        lineHeight = 18.sp
                    ),
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── Footer: Metadata ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Badge de condición de apertura
                TriggerBadge(capsule = capsule, formatter = dateFormatter)

                Spacer(Modifier.weight(1f))

                // Indicador de multimedia
                if (!capsule.mediaUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text  = stringResource(R.string.vault_has_media),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerBadge(
    capsule:   TimeCapsule,
    formatter: SimpleDateFormat
) {
    val (icon, label, tint) = when (capsule.triggerType) {
        TriggerType.IMMEDIATE_POST_MORTEM -> Triple(
            Icons.Default.AllInclusive,
            stringResource(R.string.trigger_badge_immediate),
            MaterialTheme.colorScheme.primaryContainer
        )
        TriggerType.POST_MORTEM_DATE -> Triple(
            Icons.Default.CalendarMonth,
            capsule.scheduledUnlockDate
                ?.let { formatter.format(Date(it)) }
                ?: stringResource(R.string.capsule_no_date_selected),
            MaterialTheme.colorScheme.primary
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = tint,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
