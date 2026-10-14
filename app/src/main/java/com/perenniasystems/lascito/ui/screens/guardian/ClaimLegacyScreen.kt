package com.perenniasystems.lascito.ui.screens.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.theme.AmoledBlack
import com.perenniasystems.lascito.ui.theme.TextPrimary
import com.perenniasystems.lascito.viewmodel.ClaimLegacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimLegacyScreen(
    invitationId: String,
    viewModel: ClaimLegacyViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToVault: (String) -> Unit
) {
    LaunchedEffect(invitationId) { viewModel.load(invitationId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_claim_legacy)) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock, // Placeholder for vault - requires a vault icon later
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (uiState.creatorName.isNotBlank()) {
                    stringResource(R.string.desc_claim_legacy_from, uiState.creatorName)
                } else {
                    stringResource(R.string.desc_claim_legacy)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.pin,
                onValueChange = { viewModel.onPinChanged(it) },
                label = { Text(stringResource(R.string.label_pin)) },
                placeholder = { Text("••••") },
                isError = uiState.unlockError != null,
                supportingText = {
                    val errorRes = uiState.unlockError
                    if (errorRes != null) {
                        Column {
                            Text(
                                text = stringResource(errorRes),
                                color = MaterialTheme.colorScheme.error
                            )
                            // Los intentos restantes solo aplican a un PIN incorrecto
                            if (errorRes == R.string.error_incorrect_pin && !uiState.isLocked) {
                                Text(
                                    text = stringResource(
                                        R.string.message_attempts_remaining,
                                        uiState.attemptsRemaining
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (uiState.isUnlockEnabled) viewModel.claimVault() }
                ),
                enabled = !uiState.isLocked,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.claimVault() },
                enabled = uiState.isUnlockEnabled && !uiState.isLocked,
                modifier = Modifier.fillMaxWidth() // Usa todo el width
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.action_unlock_vault))
                }
            }

            if (uiState.isLocked) {
                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.message_vault_locked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (uiState.unlockSuccess) {
        LascitoAlertDialog(
            messageRes = R.string.message_unlock_success,
            actionRes = R.string.action_view_legacy,
            onAction = {
                val creatorUid = uiState.legacyVault?.creatorUid
                viewModel.dismissSuccessDialog()
                if (creatorUid.isNullOrBlank()) onNavigateBack() else onNavigateToVault(creatorUid)
            },
            onDismiss = { viewModel.dismissSuccessDialog() }
        )
    }
}
