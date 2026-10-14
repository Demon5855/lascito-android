package com.perenniasystems.lascito.feature.settings

import com.perenniasystems.lascito.core.designsystem.component.SectionHeader
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.core.designsystem.component.LascitoAlertDialog
import com.perenniasystems.lascito.core.designsystem.theme.*
import com.perenniasystems.lascito.core.common.ValidationRules
import com.perenniasystems.lascito.feature.settings.ChangePwdState
import com.perenniasystems.lascito.feature.settings.DeleteAccountState
import com.perenniasystems.lascito.feature.settings.UserSettingsUiState
import com.perenniasystems.lascito.feature.settings.UserSettingsViewModel
import kotlinx.coroutines.launch


import com.perenniasystems.lascito.data.model.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    onNavigateBack:       () -> Unit = {},
    onNavigateToGuardian: () -> Unit = {},
    onLogOut:             () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: UserSettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // SOLUCIÓN BUG #2: Estado desde ViewModel (sobrevive recreación)
    val selectedMetal by viewModel.selectedMetal.collectAsStateWithLifecycle()
    // El idioma activo es el que Android está aplicando (también si se cambió en Ajustes del sistema)
    val selectedLang = AppLanguage.fromLocale(LocalConfiguration.current.locales[0])

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleteState) {
        if (uiState.deleteState is DeleteAccountState.Success) {
            viewModel.resetDeleteState()
            onLogOut()
        }
    }

    var showChangePwdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.changePwdState) {
        if (uiState.changePwdState is ChangePwdState.Success) {
            showChangePwdDialog = false
            viewModel.resetChangePwdState()
            onShowSnackbar(context.getString(R.string.msg_password_changed), null) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.screen_my_account),
                            style = MaterialTheme.typography.headlineLarge)
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // ── § 1 Perfil ─────────────────────────────────────────────────────
            ProfileSection(uiState)

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 2 Estado del Mausoleo ────────────────────────────────────────
            MausoleumStatusCard(uiState)

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 3 Guardián del Legado ────────────────────────────────────────
            NavigateToGuardianCard(onNavigateToGuardian)

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 4 Personalización del Santuario ──────────────────────────────
            PersonalizationSection(
                selectedLang  = selectedLang,
                selectedMetal = selectedMetal,
                onLangChange  = { lang ->
                    // Fase 1: El dropdown se cierra inmediatamente (expanded=false ya ocurrió en PersonalizationSection)
                    // Fase 2: Diferir el cambio de Locale para permitir la animación de cierre
                    scope.launch {
                        kotlinx.coroutines.delay(180) // yield + ~3 frames para la animación del Popup
                        viewModel.changeLanguage(lang)
                    }
                },
                onMetalChange = { metal ->
                    viewModel.changeMetal(metal)
                    onShowSnackbar(context.getString(R.string.toast_accent_changed,
                        context.getString(metal.labelRes)), null) {}
                }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 5 Seguridad y Privacidad ─────────────────────────────────────
            val isStrictVault by viewModel.isStrictVaultEnabled.collectAsStateWithLifecycle()
            val isBiometricLock by viewModel.isBiometricLockEnabled.collectAsStateWithLifecycle()
            val canUseBiometric = remember {
                val bm = androidx.biometric.BiometricManager.from(context)
                bm.canAuthenticate(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
            }
            SecuritySection(
                isStrictVault    = isStrictVault,
                onToggleVault    = { viewModel.toggleStrictVault(it) },
                isBiometricLock  = isBiometricLock,
                canUseBiometric  = canUseBiometric,
                onToggleBiometric = { viewModel.toggleBiometricLock(it) },
                onExport  = { onShowSnackbar(context.getString(R.string.toast_exporting), null) {} },
                onPrivacy = { onShowSnackbar(context.getString(R.string.toast_privacy_soon), null) {} },
                onChangePwd = { showChangePwdDialog = true },
                onLogOut  = onLogOut,
                onDelete  = { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlay & Dialogs 
        if (showDeleteAccountDialog) {
            val titleText = stringResource(R.string.dialog_delete_account_title)
            val bodyText = stringResource(R.string.dialog_delete_account_text)
            val btnDelete = stringResource(R.string.btn_delete_account)
            val btnCancel = stringResource(R.string.btn_cancel)
            
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                containerColor = SurfaceDark,
                titleContentColor = ErrorRed,
                textContentColor = TextPrimary,
                title = { Text(titleText) },
                text = { Text(bodyText) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAccountDialog = false
                            viewModel.deleteAccount()
                        }
                    ) {
                        Text(btnDelete, color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(btnCancel, color = TextSecondary)
                    }
                }
            )
        }

        if (uiState.deleteState is DeleteAccountState.Error) {
            val errState = uiState.deleteState as DeleteAccountState.Error
            LascitoAlertDialog(
                messageRes = errState.messageResId,
                onDismiss  = { viewModel.resetDeleteState() }
            )
        }
        
        if (uiState.changePwdState is ChangePwdState.Error) {
            val errState = uiState.changePwdState as ChangePwdState.Error
            LascitoAlertDialog(
                messageRes = errState.messageResId,
                onDismiss  = { viewModel.resetChangePwdState() }
            )
        }

        if (showChangePwdDialog) {
            var currentPass by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }

            var currentVisible by remember { mutableStateOf(false) }
            var newVisible by remember { mutableStateOf(false) }
            var confirmVisible by remember { mutableStateOf(false) }

            var currentPasswordError by remember { mutableStateOf<Int?>(null) }
            var newPasswordError by remember { mutableStateOf<Int?>(null) }
            var confirmPasswordError by remember { mutableStateOf<Int?>(null) }
            
            // Función de validación reutilizable
            val validateAndSubmit = {
                currentPasswordError = null
                newPasswordError = null
                confirmPasswordError = null

                val validateNewPass = ValidationRules.validatePassword(newPass)

                if (currentPass.isBlank()) {
                    currentPasswordError = R.string.error_enter_current_password
                } else if (validateNewPass != null) {
                    newPasswordError = validateNewPass
                } else if (currentPass == newPass) {
                    newPasswordError = R.string.error_password_same_as_current
                } else if (confirmPass.isBlank()) {
                    confirmPasswordError = R.string.error_confirm_new_password
                } else if (newPass != confirmPass) {
                    confirmPasswordError = R.string.error_passwords_no_match
                } else {
                    viewModel.changePassword(currentPass, newPass)
                }
            }

            AlertDialog(
                onDismissRequest = { 
                    if (uiState.changePwdState !is ChangePwdState.Loading) {
                        showChangePwdDialog = false
                        viewModel.resetChangePwdState()
                    }
                },
                containerColor = SurfaceDark,
                titleContentColor = TextPrimary,
                textContentColor = TextPrimary,
                title = { 
                    Text(
                        text = stringResource(R.string.title_change_password),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // ─────────────────────────────────────────────────────────────
                        // Campo: Contraseña Actual
                        // ─────────────────────────────────────────────────────────────
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { 
                                currentPass = it
                                currentPasswordError = null 
                            },
                            label = { Text(stringResource(R.string.label_current_password)) },
                            placeholder = { Text(stringResource(R.string.placeholder_password_dots)) },
                            visualTransformation = if (currentVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            trailingIcon = {
                                IconButton(onClick = { currentVisible = !currentVisible }) {
                                    Icon(
                                        imageVector = if (currentVisible) 
                                            Icons.Filled.VisibilityOff 
                                        else 
                                            Icons.Filled.Visibility,
                                        contentDescription = if (currentVisible)
                                            stringResource(R.string.desc_hide_password)
                                        else
                                            stringResource(R.string.desc_show_password),
                                        tint = TextSecondary
                                    )
                                }
                            },
                            isError = currentPasswordError != null,
                            supportingText = currentPasswordError?.let { errorResId ->
                                { 
                                    Text(
                                        text = stringResource(errorResId), 
                                        color = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ─────────────────────────────────────────────────────────────
                        // Campo: Nueva Contraseña
                        // ─────────────────────────────────────────────────────────────
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { 
                                newPass = it
                                newPasswordError = null
                                if (confirmPasswordError == R.string.error_passwords_no_match) {
                                    confirmPasswordError = null
                                }
                            },
                            label = { Text(stringResource(R.string.label_new_password)) },
                            placeholder = { Text(stringResource(R.string.placeholder_password_min)) },
                            visualTransformation = if (newVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            trailingIcon = {
                                IconButton(onClick = { newVisible = !newVisible }) {
                                    Icon(
                                        imageVector = if (newVisible) 
                                            Icons.Filled.VisibilityOff 
                                        else 
                                            Icons.Filled.Visibility,
                                        contentDescription = if (newVisible)
                                            stringResource(R.string.desc_hide_password)
                                        else
                                            stringResource(R.string.desc_show_password),
                                        tint = TextSecondary
                                    )
                                }
                            },
                            isError = newPasswordError != null,
                            supportingText = newPasswordError?.let { errorResId ->
                                { 
                                    Text(
                                        text = stringResource(errorResId), 
                                        color = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ─────────────────────────────────────────────────────────────
                        // Campo: Confirmar Nueva Contraseña
                        // ─────────────────────────────────────────────────────────────
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { 
                                confirmPass = it
                                confirmPasswordError = null
                                if (newPasswordError == R.string.error_passwords_no_match) {
                                    newPasswordError = null
                                }
                            },
                            label = { Text(stringResource(R.string.label_confirm_new_password)) },
                            placeholder = { Text(stringResource(R.string.placeholder_password_repeat)) },
                            visualTransformation = if (confirmVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { validateAndSubmit() }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) 
                                            Icons.Filled.VisibilityOff 
                                        else 
                                            Icons.Filled.Visibility,
                                        contentDescription = if (confirmVisible)
                                            stringResource(R.string.desc_hide_password)
                                        else
                                            stringResource(R.string.desc_show_password),
                                        tint = TextSecondary
                                    )
                                }
                            },
                            isError = confirmPasswordError != null,
                            supportingText = confirmPasswordError?.let { errorResId ->
                                { 
                                    Text(
                                        text = stringResource(errorResId), 
                                        color = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    val isLoading = uiState.changePwdState is ChangePwdState.Loading
                    
                    Button(
                        onClick = validateAndSubmit,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = AmoledBlack
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AmoledBlack
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isLoading) 
                                stringResource(R.string.btn_loading) 
                            else 
                                stringResource(R.string.btn_save),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showChangePwdDialog = false
                            viewModel.resetChangePwdState()
                        },
                        enabled = uiState.changePwdState !is ChangePwdState.Loading
                    ) {
                        Text(
                            text = stringResource(R.string.btn_cancel), 
                            color = TextSecondary
                        )
                    }
                }
            )
        }

        if (uiState.deleteState is DeleteAccountState.Loading || uiState.changePwdState is ChangePwdState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // Block UI interaction
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 1 — Perfil
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(state: UserSettingsUiState) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Person,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(48.dp)
            )
        }
        val nameText = state.displayNameRaw ?: state.displayNameRes?.let { stringResource(it) } ?: ""
        Text(
            text  = nameText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairDisplay,
                color      = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        val emailText = state.emailRaw ?: state.emailRes?.let { stringResource(it) } ?: ""
        Text(
            text  = emailText,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 2 — Estado del Mausoleo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MausoleumStatusCard(state: UserSettingsUiState) {
    SectionHeader(stringResource(R.string.section_mausoleum))

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_perennia_leaf_logo),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(22.dp)
                )
                Text(
                    text  = stringResource(R.string.plan_lifetime),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_storage_used),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text  = "${stringResource(state.storageUsedStrRes)} / ${stringResource(state.storageMaxStrRes)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                    )
                }
                LinearProgressIndicator(
                    progress  = { 0f }, // Phase 2: Set to 0% since we removed mocks.
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = CardBorder,
                    strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 3 — Guardián del Legado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NavigateToGuardianCard(onNavigate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(stringResource(R.string.section_guardian))

        Card(
            onClick  = onNavigate,
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier              = Modifier.padding(18.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primaryContainer,
                            androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Shield,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = stringResource(R.string.nav_card_guardian_title),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text  = stringResource(R.string.guardian_help_text),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = TextSecondary,
                            lineHeight = 17.sp
                        ),
                        maxLines = 2
                    )
                }
                Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primaryContainer,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// § 4 — Personalización del Santuario
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalizationSection(
    selectedLang:  AppLanguage,
    selectedMetal: AccentMetal,
    onLangChange:  (AppLanguage) -> Unit,
    onMetalChange: (AccentMetal) -> Unit
) {
    var langExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionHeader(stringResource(R.string.section_personalization))

        // ── Idioma ─────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text  = stringResource(R.string.label_language),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
                Text(
                    text  = stringResource(selectedLang.labelRes),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            ExposedDropdownMenuBox(
                expanded         = langExpanded,
                onExpandedChange = { langExpanded = it }
            ) {
                OutlinedButton(
                    onClick  = { langExpanded = true },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .widthIn(min = 130.dp),
                    shape  = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text  = stringResource(selectedLang.labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector        = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primaryContainer,
                        modifier           = Modifier.size(16.dp)
                    )
                }
                
                ExposedDropdownMenu(
                    expanded         = langExpanded,
                    onDismissRequest = { langExpanded = false },
                    containerColor   = MaterialTheme.colorScheme.surface,
                    modifier         = Modifier.widthIn(min = 200.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(lang.labelRes),
                                    color = if (lang == selectedLang)
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        TextPrimary
                                )
                            },
                            onClick = {
                                langExpanded = false
                                onLangChange(lang)
                            },
                            trailingIcon = {
                                if (lang == selectedLang) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Metal de Custodia ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text  = stringResource(R.string.label_accent_metal),
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AccentMetal.entries.forEach { metal ->
                    val isSelected = (metal == selectedMetal)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier            = Modifier.clickable(
                            role         = Role.RadioButton,
                            onClickLabel = stringResource(metal.labelRes),
                            onClick      = { onMetalChange(metal) }
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 36.dp else 32.dp)
                                .background(metal.color, CircleShape)
                                .then(
                                    if (isSelected)
                                        Modifier.border(2.5.dp, TextPrimary, CircleShape)
                                    else
                                        Modifier.border(1.dp, metal.dimColor, CircleShape)
                                )
                        )
                        Text(
                            text  = stringResource(metal.labelRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = if (isSelected) metal.color else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// § 5 — Seguridad y Privacidad
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecuritySection(
    isStrictVault: Boolean?,
    onToggleVault: (Boolean) -> Unit,
    isBiometricLock: Boolean?,
    canUseBiometric: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onExport:  () -> Unit,
    onPrivacy: () -> Unit,
    onChangePwd: () -> Unit,
    onLogOut:  () -> Unit,
    onDelete:  () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(stringResource(R.string.section_security))

        // ── Bóveda Estricta ──────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.title_strict_vault),
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                )
                Text(
                    text = stringResource(R.string.desc_strict_vault),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp),
                    modifier = Modifier.padding(top = 4.dp, end = 16.dp)
                )
            }
            Switch(
                checked = isStrictVault ?: false,
                onCheckedChange = { onToggleVault(it) },
                enabled = isStrictVault != null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = LocalAccentMetal.current.color,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceDark
                )
            )
        }

        // ── Bloqueo Biométrico ───────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.title_biometric_lock),
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                )
                Text(
                    text = if (canUseBiometric)
                        stringResource(R.string.desc_biometric_lock)
                    else
                        stringResource(R.string.desc_biometric_unavailable),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (canUseBiometric) TextSecondary else ErrorRed.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, end = 16.dp)
                )
            }
            Switch(
                checked = (isBiometricLock ?: false) && canUseBiometric,
                onCheckedChange = { onToggleBiometric(it) },
                enabled = canUseBiometric && isBiometricLock != null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = LocalAccentMetal.current.color,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceDark
                )
            )
        }

        HorizontalDivider(
            color     = CardBorder,
            thickness = 0.5.dp,
            modifier  = Modifier.padding(vertical = 8.dp)
        )

        SecurityButton(
            icon    = Icons.Default.FileDownload,
            label   = stringResource(R.string.btn_export),
            onClick = onExport
        )
        SecurityButton(
            icon    = Icons.Default.Policy,
            label   = stringResource(R.string.btn_privacy),
            onClick = onPrivacy
        )
        SecurityButton(
            icon    = Icons.Default.LockReset,
            label   = stringResource(R.string.title_change_password),
            onClick = onChangePwd
        )
        SecurityButton(
            icon    = Icons.AutoMirrored.Filled.ExitToApp,
            label   = stringResource(R.string.btn_logout),
            onClick = onLogOut
        )

        HorizontalDivider(
            color     = CardBorder,
            thickness = 0.5.dp,
            modifier  = Modifier.padding(vertical = 8.dp)
        )

        TextButton(
            onClick  = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector        = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint               = DangerRed,
                    modifier           = Modifier.size(20.dp)
                )
                Text(
                    text  = stringResource(R.string.btn_delete_account),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color      = DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecurityButton(
    icon:    ImageVector,
    label:   String,
    onClick: () -> Unit
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = TextSecondary,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun UserSettingsScreenPreview() {
    LascitoTheme { UserSettingsScreen() }
}

