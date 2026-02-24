package com.perenniasystems.lascito.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.viewmodel.DashboardUiState
import com.perenniasystems.lascito.viewmodel.DashboardViewModel

/**
 * Dashboard screen — shows the user's Commemorative Profiles as a scrollable list.
 *
 * Observes [DashboardViewModel.uiState] and renders three states:
 * - Loading: CircularProgressIndicator
 * - Success: LazyColumn of ProfileCard items
 * - Error:  Error message with retry (future)
 *
 * Phase 1: ViewModel injects MockProfileRepository automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text  = "Lascito",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text  = stringResource(R.string.label_profiles).uppercase(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    // User avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MutedGoldDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("U", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { /* Phase 2: Navigate to Add Profile */ },
                containerColor   = MutedGold,
                contentColor     = AmoledBlack,
                shape            = CircleShape
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = stringResource(R.string.label_add_profile)
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
                        color    = MutedGold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DashboardUiState.Success -> {
                    if (state.profiles.isEmpty()) {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding     = PaddingValues(
                                start  = 16.dp,
                                end    = 16.dp,
                                top    = 16.dp,
                                bottom = 88.dp   // leave room above FAB
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier            = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.profiles,
                                key   = { it.profileId }
                            ) { profile ->
                                ProfileCard(profile = profile)
                            }
                        }
                    }
                }

                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text      = "✦",
                            style     = MaterialTheme.typography.displayLarge.copy(color = ErrorRed)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text      = state.message,
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
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
        Text(
            text  = "✦",
            style = MaterialTheme.typography.displayLarge.copy(color = MutedGoldDim)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = "Aún no hay perfiles.\nPresiona + para crear el primero.",
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
