package com.perenniasystems.lascito.ui.screens.guardian

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.util.ValidationRules
import com.perenniasystems.lascito.viewmodel.GuardianViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    viewModel:          GuardianViewModel = viewModel(),
    onNavigateBack:     () -> Unit        = {},
    onNavigateToCapsule: () -> Unit       = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastLegacySaved = stringResource(R.string.toast_legacy_saved)

    val isGuardianSectionValid = ValidationRules.validateName(uiState.guardianName) == null &&
            ValidationRules.validateEmail(uiState.guardianEmail) == null &&
            ValidationRules.validatePin(uiState.guardianPin) == null

    // Feedback de guardado exitoso → snackbar
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onShowSnackbar(toastLegacySaved, null) {}
            viewModel.clearSaved()
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
                        Text(
                            text  = stringResource(R.string.screen_guardian),
                            style = MaterialTheme.typography.headlineLarge
                        )
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
                actions = { Spacer(Modifier.width(48.dp)) },
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // A — Beneficiario / Heredero
            BeneficiaryCard(
                name    = uiState.guardianName,
                email   = uiState.guardianEmail,
                pin     = uiState.guardianPin,
                pinHint = uiState.guardianPinHint,
                pinHintError = uiState.pinHintError,
                onName  = { viewModel.onNameChange(it) },
                onEmail = { viewModel.onEmailChange(it) },
                onPin   = { viewModel.onPinChange(it) },
                onPinHint = { viewModel.onPinHintChange(it) }
            )

            // ── Tarjeta B: Protocolo de Ausencia ───────────────────────────────
            DeadManSwitchCard(
                isActive       = uiState.isSwitchActive,
                inactivityDays = uiState.inactivityDays,
                onToggle       = viewModel::onSwitchChange,
                onDaysChange   = viewModel::onDaysChange
            )

            // ── Botón Guardar ──────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveGuardianConfig() },
                enabled  = !uiState.isLoading && isGuardianSectionValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = AmoledBlack
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = AmoledBlack
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(
                        if (uiState.isLoading) R.string.btn_saving else R.string.btn_save_legacy_config
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }

            // ── CTA: Bóveda de Cápsulas ──────────────────────────────────────────
            OutlinedButton(
                onClick  = onNavigateToCapsule,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.primaryContainer
                ),
                colors  = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = stringResource(R.string.screen_capsule_vault),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp
                )
            }

            // ── Footer: Estado del Sistema ───────────────────────────────────────────
            SystemStatusFooter(
                lastProofOfLife = uiState.lastProofOfLife,
                onManualCheck   = { viewModel.triggerManualCheck() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // Error dialog: muestra el mensaje concreto (validación, red, genérico...)
    uiState.error?.let { errorRes ->
        LascitoAlertDialog(
            messageRes = errorRes,
            onDismiss  = { viewModel.clearError() }
        )
    }

    // Cooldown Anti-Spam dialog
    if (uiState.showCooldownDialog) {
        AlertDialog(
            onDismissRequest  = { viewModel.dismissCooldownDialog() },
            containerColor    = SurfaceDark,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor  = TextPrimary,
            title = { Text(stringResource(R.string.title_cooldown_active)) },
            text  = {
                Text(
                    stringResource(
                        R.string.message_guardian_cooldown,
                        uiState.cooldownDaysRemaining
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCooldownDialog() }) {
                    Text(stringResource(R.string.action_understood), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A — Tarjeta "El Heredero"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BeneficiaryCard(
    name:    String,
    email:   String,
    pin:     String,
    pinHint: String,
    pinHintError: Int?,
    onName:  (String) -> Unit,
    onEmail: (String) -> Unit,
    onPin:   (String) -> Unit,
    onPinHint: (String) -> Unit
) {
    var showHintHelpDialog by remember { mutableStateOf(false) }

    LegacyCard {
        // Encabezado
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.PersonPin,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text  = stringResource(R.string.section_beneficiary),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color      = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlayfairDisplay
                    )
                )
                Text(
                    text  = stringResource(R.string.desc_beneficiary_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 20.dp))

        // Campos
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LegacyTextField(
                value       = name,
                onChange    = onName,
                label       = stringResource(R.string.label_heir_name),
                placeholder = stringResource(R.string.placeholder_heir_name),
                icon        = Icons.Default.Person,
                keyType     = KeyboardType.Text
            )

            LegacyTextField(
                value       = email,
                onChange    = onEmail,
                label       = stringResource(R.string.label_heir_email),
                placeholder = stringResource(R.string.placeholder_heir_email),
                icon        = Icons.Default.Email,
                keyType     = KeyboardType.Email
            )

            // Campo PIN
            OutlinedTextField(
                value         = pin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onPin(it) },
                label         = { Text(stringResource(R.string.label_heir_pin), color = TextSecondary) },
                placeholder   = { Text(stringResource(R.string.placeholder_new_heir_pin), color = TextDisabled) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                leadingIcon   = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = legacyTextFieldColors(),
                shape         = RoundedCornerShape(10.dp),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )

            // Nota UX
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text  = stringResource(R.string.guardian_key_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }

            // PIN Hint Dinámico
            AnimatedVisibility(visible = pin.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.label_heir_pin_hint),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { showHintHelpDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Ayuda",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.desc_heir_pin_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = pinHint,
                        onValueChange = onPinHint,
                        label = { Text(stringResource(R.string.placeholder_heir_pin_hint), color = TextDisabled) },
                        isError = pinHintError != null,
                        supportingText = {
                            if (pinHintError != null) {
                                Text(
                                    text = stringResource(pinHintError),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.label_pin_hint_length, pinHint.length),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pinHint.length >= 15) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = legacyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )

                    HintExamplesCard()
                }
            }
        }
    }

    if (showHintHelpDialog) {
        LascitoAlertDialog(
            titleRes = R.string.title_what_is_pin_hint,
            messageRes = R.string.message_what_is_pin_hint_body,
            onDismiss = { showHintHelpDialog = false }
        )
    }
}

@Composable
private fun HintExamplesCard() {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.label_good_hint_example),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "\"${stringResource(R.string.example_heir_pin_hint)}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.label_bad_hint_example),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(R.string.example_bad_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// B — Tarjeta "Protocolo de Ausencia"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeadManSwitchCard(
    isActive:       Boolean,
    inactivityDays: Int,
    onToggle:       (Boolean) -> Unit,
    onDaysChange:   (Int) -> Unit
) {
    val dayOptions = listOf(30, 90, 180, 365)

    LegacyCard {
        // Encabezado
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Schedule,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text  = stringResource(R.string.section_absence_protocol),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color      = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlayfairDisplay
                    )
                )
                Text(
                    text  = stringResource(R.string.desc_absence_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Selector de inactividad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text  = stringResource(R.string.label_inactivity_threshold),
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayOptions.forEach { days ->
                        val isSelected = days == inactivityDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else SurfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else CardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onDaysChange(days) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = "${days}d",
                                color      = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextSecondary,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Switch maestro
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = stringResource(R.string.label_switch_active),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text  = stringResource(
                            if (isActive) R.string.desc_switch_active_on else R.string.desc_switch_inactive
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                Switch(
                    checked         = isActive,
                    onCheckedChange = onToggle,
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = AmoledBlack,
                        checkedTrackColor   = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = CardBorder
                    )
                )
            }

            // Texto explicativo dinámico
            Text(
                text  = stringResource(R.string.guardian_inactivity_subtitle, inactivityDays),
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = TextSecondary,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// C — Footer: Estado del Sistema
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SystemStatusFooter(
    lastProofOfLife: Long,
    onManualCheck:   () -> Unit
) {
    // Pulso animado
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Formateo inyectado en un LaunchEffect para ser despachado por el Scheduler de Compose cada minuto (60,000 ms)
    // y no colapsar la memoria del CPU y recolector de basura
    val justNow = stringResource(R.string.heartbeat_just_now)
    val never = stringResource(R.string.heartbeat_never)
    val secondsSingular = stringResource(R.string.heartbeat_seconds, 0)
    val secondsPlural = stringResource(R.string.heartbeat_seconds_plural, 0)
    val minutesSingular = stringResource(R.string.heartbeat_minutes, 0)
    val minutesPlural = stringResource(R.string.heartbeat_minutes_plural, 0)
    val hoursSingular = stringResource(R.string.heartbeat_hours, 0)
    val hoursPlural = stringResource(R.string.heartbeat_hours_plural, 0)
    val daysSingular = stringResource(R.string.heartbeat_days, 0)
    val daysPlural = stringResource(R.string.heartbeat_days_plural, 0)

    val timeSinceText = remember(lastProofOfLife) {
        formatRelativeTime(
            timestampMs = lastProofOfLife,
            justNow = justNow,
            never = never,
            secondsSingular = secondsSingular,
            secondsPlural = secondsPlural,
            minutesSingular = minutesSingular,
            minutesPlural = minutesPlural,
            hoursSingular = hoursSingular,
            hoursPlural = hoursPlural,
            daysSingular = daysSingular,
            daysPlural = daysPlural
        )
    }

    var relativeTime by remember { mutableStateOf(timeSinceText) }

    LaunchedEffect(lastProofOfLife) {
        while (true) {
            relativeTime = formatRelativeTime(
                timestampMs = lastProofOfLife,
                justNow = justNow,
                never = never,
                secondsSingular = secondsSingular,
                secondsPlural = secondsPlural,
                minutesSingular = minutesSingular,
                minutesPlural = minutesPlural,
                hoursSingular = hoursSingular,
                hoursPlural = hoursPlural,
                daysSingular = daysSingular,
                daysPlural = daysPlural
            )
            kotlinx.coroutines.delay(60_000L) // 1 recomposición/minuto
        }
    }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier.fillMaxWidth()
        ) {
            // Punto pulsante
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                        )
                    )
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text  = stringResource(R.string.label_system_operative),
                style = MaterialTheme.typography.labelLarge.copy(
                    color      = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text  = "  ·",
                style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
            )

            Text(
                text  = "  " + stringResource(R.string.label_last_heartbeat, relativeTime),
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
            )
        }

        // Botón de Prueba de Vida Manual
        OutlinedButton(
            onClick = onManualCheck,
            shape   = RoundedCornerShape(8.dp),
            border  = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.primaryContainer
            ),
            colors  = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = stringResource(R.string.btn_send_manual_signal),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LegacyCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content  = content
        )
    }
}

@Composable
private fun LegacyTextField(
    value:       String,
    onChange:    (String) -> Unit,
    label:       String,
    placeholder: String,
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    keyType:     KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onChange,
        label           = { Text(label, color = TextSecondary) },
        placeholder     = { Text(placeholder, color = TextDisabled) },
        leadingIcon     = {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyType),
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        colors          = legacyTextFieldColors(),
        shape           = RoundedCornerShape(10.dp),
        textStyle       = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
    )
}

@Composable
private fun legacyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primaryContainer,
    unfocusedBorderColor    = CardBorder,
    cursorColor             = MaterialTheme.colorScheme.primary,
    focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
    unfocusedContainerColor = Color.Transparent
)

// ─────────────────────────────────────────────────────────────────────────────
// Helper — relative time (now receives pre-resolved strings from Composable)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Calcula el tiempo relativo usando las cadenas ya resueltas por [stringResource].
 * Los parámetros *template reciben el string con valor 0 únicamente para validar
 * el format; la función los reformatea con el valor real.
 */
private fun formatRelativeTime(
    timestampMs:     Long,
    justNow:         String,
    never:           String,
    secondsSingular: String,
    secondsPlural:   String,
    minutesSingular: String,
    minutesPlural:   String,
    hoursSingular:   String,
    hoursPlural:     String,
    daysSingular:    String,
    daysPlural:      String
): String {
    if (timestampMs == 0L) return never
    val diffMs      = System.currentTimeMillis() - timestampMs
    if (diffMs < 60_000L) return justNow
    val diffSeconds = diffMs / 1_000
    val diffMinutes = diffSeconds / 60
    val diffHours   = diffMinutes / 60
    val diffDays    = diffHours / 24
    return when {
        diffSeconds < 60  -> if (diffSeconds == 1L) secondsSingular.replace("0", "1")
                             else secondsPlural.replace("0", diffSeconds.toString())
        diffMinutes < 60  -> if (diffMinutes == 1L) minutesSingular.replace("0", "1")
                             else minutesPlural.replace("0", diffMinutes.toString())
        diffHours   < 24  -> if (diffHours == 1L)   hoursSingular.replace("0", "1")
                             else hoursPlural.replace("0", diffHours.toString())
        else              -> if (diffDays == 1L)    daysSingular.replace("0", "1")
                             else daysPlural.replace("0", diffDays.toString())
    }
}
