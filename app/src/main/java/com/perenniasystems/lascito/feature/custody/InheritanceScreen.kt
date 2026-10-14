package com.perenniasystems.lascito.feature.custody

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.Legacy
import com.perenniasystems.lascito.core.designsystem.component.LascitoAlertDialog
import com.perenniasystems.lascito.feature.legacy.LegacyCard
import com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack
import com.perenniasystems.lascito.core.designsystem.theme.LascitoTheme
import com.perenniasystems.lascito.core.designsystem.theme.TextPrimary
import com.perenniasystems.lascito.feature.custody.InheritanceUiState
import com.perenniasystems.lascito.feature.custody.InheritanceViewModel

@Composable
fun InheritanceRoute(
    creatorUid: String,
    onNavigateBack: () -> Unit,
    onOpenLegacy: (String) -> Unit,
    viewModel: InheritanceViewModel = hiltViewModel()
) {
    LaunchedEffect(creatorUid) { viewModel.load(creatorUid) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InheritanceContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onOpenLegacy = onOpenLegacy
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InheritanceContent(
    uiState: InheritanceUiState,
    onNavigateBack: () -> Unit,
    onOpenLegacy: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_inheritance)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = AmoledBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is InheritanceUiState.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is InheritanceUiState.Error -> {
                    LascitoAlertDialog(
                        messageRes = uiState.messageResId,
                        onDismiss = onNavigateBack
                    )
                }

                is InheritanceUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.inheritance_from, uiState.creatorName),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primaryContainer
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )

                        if (uiState.legacies.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_inheritance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.legacies,
                                    key = { it.profileId }
                                ) { legacy ->
                                    LegacyCard(
                                        legacy = legacy,
                                        readOnly = true,
                                        onClick = { onOpenLegacy(legacy.profileId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun InheritanceContentPreview() {
    LascitoTheme {
        InheritanceContent(
            uiState = InheritanceUiState.Success(
                creatorName = "Elena Ríos",
                legacies = listOf(
                    Legacy(profileId = "1", fullName = "Abuelo Tomás", relationship = "Abuelo"),
                    Legacy(profileId = "2", fullName = "Luna", relationship = "Mascota")
                )
            ),
            onNavigateBack = {},
            onOpenLegacy = {}
        )
    }
}
