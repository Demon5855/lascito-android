package com.perenniasystems.lascito.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.viewmodel.DashboardUiState
import com.perenniasystems.lascito.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToAltar:        (String) -> Unit = {},
    onNavigateToAddProfile:   (String?) -> Unit = {},
    onNavigateToUserSettings: () -> Unit = {},
    onNavigateToClaimLegacy:  (String) -> Unit = {},
    onNavigateToInheritance:  (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmailVerified by viewModel.isEmailVerified.collectAsStateWithLifecycle()
    val isSendingVerification by viewModel.isSendingVerification.collectAsStateWithLifecycle()
    val verificationEmailSent by viewModel.verificationEmailSent.collectAsStateWithLifecycle()
    var profileToDelete by remember { mutableStateOf<com.perenniasystems.lascito.data.model.Profile?>(null) }

    // Trigger re-fetch every time this screen becomes active
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(verificationEmailSent) {
        if (verificationEmailSent) {
            kotlinx.coroutines.delay(3000L)
            viewModel.resetVerificationBannerState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = stringResource(R.string.screen_my_altars),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                // Spacer igual al ancho del �cono de la derecha ? t�tulo verdaderamente centrado
                navigationIcon = { Spacer(modifier = Modifier.width(48.dp)) },
                // Settings icon as counterweight for the centered title
                actions = {
                    IconButton(onClick = onNavigateToUserSettings) {
                        Icon(
                            imageVector        = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.screen_my_account),
                            tint               = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(
                    containerColor    = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { onNavigateToAddProfile(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = AmoledBlack,
                shape          = CircleShape,
                elevation      = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = stringResource(R.string.desc_create_altar)
                )
            }
        },
        containerColor = AmoledBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isEmailVerified) {
                val bannerMessage = if (verificationEmailSent) {
                    stringResource(R.string.toast_verification_sent)
                } else {
                    stringResource(R.string.banner_verify_email)
                }

                InfoBanner(
                    message = bannerMessage,
                    actionText = stringResource(R.string.action_resend),
                    isLoading = isSendingVerification,
                    isSuccess = verificationEmailSent,
                    onActionClick = { viewModel.resendVerificationEmail() }
                )
            }
            
            // Phase 4.1: Mostrar banner de reclamo solo si el correo está verificado y hay legados
            if (isEmailVerified) {
                val state = uiState
                if (state is DashboardUiState.Success) {
                    state.pendingInvitations.forEach { invitation ->
                        InfoBanner(
                            message = if (invitation.creatorName.isNotBlank()) {
                                stringResource(R.string.banner_pending_legacy_from, invitation.creatorName)
                            } else {
                                stringResource(R.string.banner_pending_legacy)
                            },
                            actionText = stringResource(R.string.action_claim_legacy),
                            onActionClick = { onNavigateToClaimLegacy(invitation.id) }
                        )
                    }
                    state.claimedInheritances.forEach { inheritance ->
                        InfoBanner(
                            message = stringResource(R.string.banner_claimed_inheritance, inheritance.creatorName),
                            actionText = stringResource(R.string.action_view_inheritance),
                            onActionClick = { onNavigateToInheritance(inheritance.creatorUid) }
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {

                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DashboardUiState.Success -> {
                    if (state.profiles.isEmpty()) {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns             = GridCells.Fixed(2),
                            contentPadding      = PaddingValues(
                                start  = 16.dp,
                                end    = 16.dp,
                                top    = 20.dp,
                                bottom = 96.dp  // respira por encima del FAB
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            modifier              = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.profiles,
                                key   = { it.profileId }
                            ) { profile ->
                                ProfileCard(
                                    profile = profile,
                                    onClick = { onNavigateToAltar(profile.profileId) },
                                    onEdit = { 
                                        onNavigateToAddProfile(profile.profileId)
                                    },
                                    onDelete = { profileToDelete = profile }
                                )
                            }
                        }
                    }
                }

                is DashboardUiState.Error -> {
                    LascitoAlertDialog(
                        messageRes = state.messageResId,
                        onDismiss  = { viewModel.resetState() }
                    )
                }
            }
        } // Closure for the Box holding the `when(state)`

            if (profileToDelete != null) {
                AlertDialog(
                    onDismissRequest = { profileToDelete = null },
                    containerColor = SurfaceDark,
                    titleContentColor = ErrorRed,
                    textContentColor = TextPrimary,
                    title = { Text(stringResource(R.string.confirm_delete_profile_title)) },
                    text = { Text(stringResource(R.string.confirm_delete_profile_text, profileToDelete?.fullName ?: "")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                profileToDelete?.let {
                                    viewModel.deleteProfile(it.profileId, it.avatarUrl)
                                }
                                profileToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.action_delete), color = ErrorRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { profileToDelete = null }) {
                            Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InfoBanner(
    message: String,
    actionText: String,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    onActionClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isSuccess) ThemeState.selectedMetal.color else TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        if (!isSuccess) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = ThemeState.selectedMetal.color,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
            } else {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge.copy(color = ThemeState.selectedMetal.color)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_perennia_leaf_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = stringResource(R.string.empty_altars),
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun DashboardScreenPreview() {
    LascitoTheme { DashboardScreen() }
}
