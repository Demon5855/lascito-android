package com.perenniasystems.lascito.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.util.LocaleHelper
import com.perenniasystems.lascito.util.Validators
import com.perenniasystems.lascito.data.repository.ThemeRepository
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.viewmodel.UserSettingsUiState
import com.perenniasystems.lascito.viewmodel.UserSettingsViewModel
import com.perenniasystems.lascito.viewmodel.DeleteAccountState

// ── Language options ──────────────────────────────────────────────────────────
enum class AppLanguage(val labelRes: Int, val tag: String) {
    SPANISH(R.string.lang_spanish, "es"),
    ENGLISH(R.string.lang_english, "en")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onLogOut:       () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: UserSettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val prefs = com.perenniasystems.lascito.data.local.OnboardingPreferences(context.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return UserSettingsViewModel(prefs = prefs) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val themeRepository = remember { ThemeRepository(context) }

    // Init from LangState (which was already seeded from SharedPreferences
    // in MainActivity.onCreate before setContent was called).
    var selectedLang by remember {
        mutableStateOf(
            AppLanguage.entries.firstOrNull { it.tag == LangState.selectedTag }
                ?: AppLanguage.SPANISH
        )
    }

    // Accent metal state lives in the singleton ThemeState — reading it here
    // means this composable stays in sync with any external changes, and writing
    // propagates instantly through LascitoTheme to the whole tree.
    val selectedMetal = ThemeState.selectedMetal

    var guardianEmail by remember { mutableStateOf("") }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleteState) {
        if (uiState.deleteState is DeleteAccountState.Success) {
            viewModel.resetDeleteState()
            onLogOut()
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
            GuardianSection(
                email         = guardianEmail,
                onEmailChange = { guardianEmail = it },
                onSave        = { onShowSnackbar(context.getString(R.string.toast_guardian_saved), null) {} }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 4 Personalización del Santuario ──────────────────────────────
            PersonalizationSection(
                selectedLang  = selectedLang,
                selectedMetal = selectedMetal,
                onLangChange  = { lang ->
                    selectedLang = lang
                    LocaleHelper.save(context, lang.tag)
                    java.util.Locale.setDefault(java.util.Locale.forLanguageTag(lang.tag))
                    LangState.selectedTag = lang.tag
                    
                    val localeList = LocaleListCompat.forLanguageTags(lang.tag)
                    AppCompatDelegate.setApplicationLocales(localeList)
                },
                onMetalChange = { metal ->
                    ThemeState.selectedMetal = metal   // triggers recomposition everywhere
                    scope.launch {
                        themeRepository.saveThemeColor(metal.name)
                    }
                    onShowSnackbar(context.getString(R.string.toast_accent_changed,
                        context.getString(metal.labelRes)), null) {}
                }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // ── § 5 Seguridad y Privacidad ─────────────────────────────────────
            val isStrictVault by viewModel.isStrictVaultEnabled?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }
            SecuritySection(
                isStrictVault = isStrictVault,
                onToggleVault = { viewModel.toggleStrictVault(it) },
                onExport  = { onShowSnackbar(context.getString(R.string.toast_exporting), null) {} },
                onPrivacy = { onShowSnackbar(context.getString(R.string.toast_privacy_soon), null) {} },
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

        if (uiState.deleteState is DeleteAccountState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // Block UI interaction
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DangerRed)
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
    SectionTitle(stringResource(R.string.section_mausoleum))

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
private fun GuardianSection(
    email:         String,
    onEmailChange: (String) -> Unit,
    onSave:        () -> Unit
) {
    // Same email-format check used on Login and Register screens.
    val emailError = email.isNotBlank() && !Validators.EMAIL_REGEX.matches(email)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(stringResource(R.string.section_guardian))

        OutlinedTextField(
            value         = email,
            onValueChange = onEmailChange,
            modifier      = Modifier.fillMaxWidth(),
            isError       = emailError,
            placeholder   = {
                Text(
                    stringResource(R.string.hint_guardian_email),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled)
                )
            },
            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Default.Shield,
                    contentDescription = null,
                    tint               = if (emailError)
                        ErrorRed
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    modifier           = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = MaterialTheme.colorScheme.primaryContainer,
                unfocusedBorderColor    = CardBorder,
                errorBorderColor        = ErrorRed,
                errorLeadingIconColor   = ErrorRed,
                cursorColor             = MaterialTheme.colorScheme.primary,
                focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp)
        )

        if (emailError) {
            Text(
                text     = stringResource(R.string.error_invalid_email),
                color    = ErrorRed,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Text(
            text  = stringResource(R.string.guardian_help_text),
            style = MaterialTheme.typography.bodySmall.copy(
                color      = TextSecondary,
                lineHeight = 18.sp
            )
        )

        // Save button visible only when email is entered and passes validation.
        if (email.isNotBlank() && !emailError) {
            TextButton(
                onClick  = onSave,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    stringResource(R.string.btn_save_guardian),
                    color = MaterialTheme.colorScheme.primary
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
        SectionTitle(stringResource(R.string.section_personalization))

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
                        contentColor = MaterialTheme.colorScheme.primary),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text  = stringResource(selectedLang.labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary)
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
                    containerColor   = SurfaceDark
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(lang.labelRes),
                                    color = if (lang == selectedLang)
                                        MaterialTheme.colorScheme.primary else TextPrimary
                                )
                            },
                            onClick = {
                                onLangChange(lang)
                                langExpanded = false
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
    isStrictVault: Boolean,
    onToggleVault: (Boolean) -> Unit,
    onExport:  () -> Unit,
    onPrivacy: () -> Unit,
    onLogOut:  () -> Unit,
    onDelete:  () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.section_security))

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
                checked = isStrictVault,
                onCheckedChange = { onToggleVault(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = ThemeState.selectedMetal.color,
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
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = PlayfairDisplay,
            color      = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    )
}

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

