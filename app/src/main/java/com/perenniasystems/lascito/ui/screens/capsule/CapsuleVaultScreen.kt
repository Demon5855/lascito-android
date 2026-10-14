package com.perenniasystems.lascito.ui.screens.capsule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.TimeCapsule
import com.perenniasystems.lascito.data.model.TriggerType
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.viewmodel.CapsuleVaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleVaultScreen(
    viewModel: CapsuleVaultViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var capsuleToDelete by remember { mutableStateOf<TimeCapsule?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadCapsules()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.screen_capsule_vault),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = { Spacer(Modifier.width(48.dp)) }, // Centrado visual del título
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = AmoledBlack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_new_capsule_fab)
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
            if (!uiState.isLoading && uiState.capsules.isEmpty()) {
                // ── Empty State ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = SurfaceDark, // Color muy sutil
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.vault_empty_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            } else {
                // ── Lista de Cápsulas ──
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.capsules, key = { it.capsuleId }) { capsule ->
                        CapsuleItemCard(
                            capsule = capsule,
                            onClick = { onNavigateToDetail(capsule.capsuleId) },
                            onDeleteClick = { capsuleToDelete = capsule }
                        )
                    }
                }
            }

            // ── Loading Overlay (opcional si queremos bloquear interactividad rápida) ──
            if (uiState.isLoading && uiState.capsules.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // ── Dialog de Eliminación ──
    capsuleToDelete?.let { capsule ->
        AlertDialog(
            onDismissRequest = { capsuleToDelete = null },
            containerColor = SurfaceDark,
            titleContentColor = ErrorRed,
            textContentColor = TextPrimary,
            title = { Text(stringResource(R.string.dialog_delete_capsule_title)) },
            text = { Text(stringResource(R.string.dialog_delete_capsule_body, capsule.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCapsule(capsule)
                    capsuleToDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { capsuleToDelete = null }) {
                    Text(text = stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            }
        )
    }

    // ── Error Dialog ──
    uiState.error?.let { errorRes ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = SurfaceDark,
            titleContentColor = ErrorRed,
            textContentColor = TextPrimary,
            title = { Text(stringResource(R.string.error_generic)) },
            text = { Text(stringResource(errorRes)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(R.string.btn_understood), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun CapsuleItemCard(
    capsule: TimeCapsule,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header (Title)
                Text(
                    text = capsule.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                // Body (Preview)
                Text(
                    text = capsule.content ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                // Footer (Metadata)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (capsule.mediaUrl != null) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachment",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isImmediate = capsule.triggerType == TriggerType.IMMEDIATE_POST_MORTEM
                        Icon(
                            imageVector = if (isImmediate) Icons.Default.LockClock else Icons.Default.CalendarToday,
                            contentDescription = "Trigger Type",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isImmediate) {
                                stringResource(R.string.trigger_badge_immediate)
                            } else {
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                val dateStr = capsule.scheduledUnlockDate?.let { sdf.format(Date(it)) } ?: "—"
                                stringResource(R.string.trigger_date_format, dateStr)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Trailing Action (Delete)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = ErrorRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}
