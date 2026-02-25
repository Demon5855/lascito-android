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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.viewmodel.DashboardUiState
import com.perenniasystems.lascito.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToAltar:        (String) -> Unit = {},
    onNavigateToAddProfile:   (String?) -> Unit = {},
    onNavigateToUserSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var profileToDelete by remember { mutableStateOf<com.perenniasystems.lascito.data.model.Profile?>(null) }

    // Trigger re-fetch every time this screen becomes active
    LaunchedEffect(Unit) {
        viewModel.refresh()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
