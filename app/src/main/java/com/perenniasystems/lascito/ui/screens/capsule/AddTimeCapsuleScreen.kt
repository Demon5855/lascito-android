package com.perenniasystems.lascito.ui.screens.capsule

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.model.TriggerType
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.viewmodel.TimeCapsuleViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

private const val MAX_CONTENT_LENGTH = 2000

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeCapsuleScreen(
    viewModel:      TimeCapsuleViewModel = viewModel(),
    onNavigateBack: () -> Unit           = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val successMsg = stringResource(R.string.toast_capsule_saved)

    // ── Promo biométrico (one-shot) ─────────────────────────────────────────
    val onboardingPreferences = remember { com.perenniasystems.lascito.data.local.OnboardingPreferences(context) }
    var showBiometricPromo by remember { mutableStateOf(false) }
    var pendingNavigateBack by remember { mutableStateOf(false) }

    val canUseBiometric = remember {
        val bm = androidx.biometric.BiometricManager.from(context)
        bm.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    // Navegar atrás al guardar (con posible promo)
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onShowSnackbar(successMsg, null) {}
            viewModel.resetSuccess()

            // Evaluar si mostrar promo biométrico
            val hasSeen = try {
                onboardingPreferences.hasSeenBiometricPromo.first()
            } catch (_: Exception) { true }

            val biometricAlreadyOn = try {
                onboardingPreferences.isBiometricLockEnabled.first()
            } catch (_: Exception) { false }

            if (!hasSeen && canUseBiometric && !biometricAlreadyOn) {
                showBiometricPromo = true
            } else {
                onNavigateBack()
            }
        }
    }

    // Media pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.onMediaSelected(uri, MediaType.IMAGE) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.onMediaSelected(uri, MediaType.VIDEO) }

    // DatePicker state
    var showDatePicker by remember { mutableStateOf(false) }
    val currentLangTag = LangState.selectedTag

    // BottomSheet para elegir tipo de media
    var showMediaSheet by remember { mutableStateOf(false) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Validation
    val isDateRequired = uiState.triggerType == TriggerType.POST_MORTEM_DATE
    val canSave        = uiState.title.isNotBlank() &&
                         !(isDateRequired && uiState.scheduledDate == null) &&
                         !uiState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = stringResource(R.string.screen_new_capsule),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.primary
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
        Box(Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // ── Sección 1: El Mensaje ──────────────────────────────────────
                MessageSection(
                    title      = uiState.title,
                    content    = uiState.content,
                    onTitle    = viewModel::onTitleChange,
                    onContent  = viewModel::onContentChange
                )

                // ── Sección 2: Multimedia ──────────────────────────────────────
                MediaSection(
                    mediaUri   = uiState.mediaUri,
                    mediaType  = uiState.mediaType,
                    onAttach   = { showMediaSheet = true },
                    onClear    = { viewModel.clearMedia() }
                )

                // ── Sección 3: Condición de Apertura ───────────────────────────
                TriggerSection(
                    triggerType    = uiState.triggerType,
                    scheduledDate  = uiState.scheduledDate,
                    onTrigger      = viewModel::onTriggerTypeChange,
                    onPickDate     = { showDatePicker = true }
                )

                // ── Botón Guardar ──────────────────────────────────────────────
                Button(
                    onClick  = { viewModel.saveCapsule() },
                    enabled  = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        contentColor           = AmoledBlack,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor   = TextDisabled
                    )
                ) {
                    Text(
                        text       = stringResource(R.string.btn_seal_capsule),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Overlay de carga ───────────────────────────────────────────────
            if (uiState.isLoading) {
                CapsuleLoadingOverlay(progress = uiState.uploadProgress)
            }
        }
    }

    // ── DatePickerDialog (con forzado de Locale) ──────────────────────────────
    if (showDatePicker) {
        key(currentLangTag) {
            val contextLoc = LocalContext.current
            val locale = java.util.Locale(currentLangTag)
            val configuration = android.content.res.Configuration(contextLoc.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = contextLoc.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration
            ) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.scheduledDate
                        ?: (System.currentTimeMillis() + 86_400_000L)
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton    = {
                        TextButton(onClick = {
                            viewModel.onScheduledDateChange(datePickerState.selectedDateMillis)
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.btn_ok), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                        }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor       = SurfaceDark,
                        titleContentColor    = TextPrimary,
                        headlineContentColor = MaterialTheme.colorScheme.primary,
                        weekdayContentColor  = TextSecondary,
                        dayContentColor      = TextPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        selectedDayContentColor   = AmoledBlack,
                        todayDateBorderColor      = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    DatePicker(
                        state  = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = SurfaceDark
                        )
                    )
                }
            }
        }
    }

    // ── Promo biométrico dialog ──────────────────────────────────────────────
    if (showBiometricPromo) {
        AlertDialog(
            onDismissRequest = {
                showBiometricPromo = false
                coroutineScope.launch {
                    onboardingPreferences.saveBiometricPromoSeen()
                    onNavigateBack()
                }
            },
            containerColor    = SurfaceDark,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor  = TextPrimary,
            icon = {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text(stringResource(R.string.promo_biometric_title)) },
            text  = { Text(stringResource(R.string.promo_biometric_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBiometricPromo = false
                    coroutineScope.launch {
                        onboardingPreferences.saveBiometricLockState(true)
                        onboardingPreferences.saveBiometricPromoSeen()
                        onNavigateBack()
                    }
                }) {
                    Text(
                        stringResource(R.string.promo_biometric_activate),
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBiometricPromo = false
                    coroutineScope.launch {
                        onboardingPreferences.saveBiometricPromoSeen()
                        onNavigateBack()
                    }
                }) {
                    Text(
                        stringResource(R.string.promo_biometric_later),
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // ── Error dialog ───────────────────────────────────────────────────────────
    uiState.errorMessageResId?.let { msgResId ->
        LascitoAlertDialog(
            messageRes = msgResId,
            onDismiss  = { viewModel.clearError() },
            actionRes  = if (msgResId == R.string.error_email_not_verified) R.string.action_resend else null,
            onAction   = if (msgResId == R.string.error_email_not_verified) {
                {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    user?.sendEmailVerification()
                    viewModel.clearError()
                }
            } else null
        )
    }

    // ── Media BottomSheet ──────────────────────────────────────────────────────
    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1A1A1A),
            shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                CapsuleMediaSheetItem(
                    icon  = Icons.Default.PhotoCamera,
                    label = stringResource(R.string.action_add_photo),
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showMediaSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                )
                CapsuleMediaSheetItem(
                    icon  = Icons.Default.Videocam,
                    label = stringResource(R.string.action_add_video),
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showMediaSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 1 — El Mensaje
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageSection(
    title:    String,
    content:  String,
    onTitle:  (String) -> Unit,
    onContent:(String) -> Unit
) {
    CapsuleCard {
        // Header
        SectionHeader(
            icon    = Icons.Default.EditNote,
            title   = stringResource(R.string.capsule_section_message),
            subtitle = stringResource(R.string.capsule_section_message_sub)
        )

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Título de la cápsula
            OutlinedTextField(
                value         = title,
                onValueChange = onTitle,
                label         = { Text(stringResource(R.string.label_capsule_title), color = TextSecondary) },
                placeholder   = { Text(stringResource(R.string.placeholder_capsule_title), color = TextDisabled) },
                leadingIcon   = {
                    Icon(Icons.Default.Title, null,
                        tint     = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(20.dp))
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = capsuleFieldColors(),
                shape         = RoundedCornerShape(10.dp),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )

            // Carta / Contenido
            OutlinedTextField(
                value         = content,
                onValueChange = { if (it.length <= MAX_CONTENT_LENGTH) onContent(it) },
                label         = { Text(stringResource(R.string.label_capsule_content), color = TextSecondary) },
                placeholder   = {
                    Text(
                        text  = stringResource(R.string.placeholder_capsule_content),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color    = TextDisabled,
                            fontSize = 14.sp
                        )
                    )
                },
                minLines      = 5,
                maxLines      = 12,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier      = Modifier.fillMaxWidth(),
                colors        = capsuleFieldColors(),
                shape         = RoundedCornerShape(10.dp),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color      = TextPrimary,
                    lineHeight = 22.sp
                ),
                supportingText = {
                    Text(
                        text      = "${content.length} / $MAX_CONTENT_LENGTH",
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style     = MaterialTheme.typography.labelSmall.copy(color = TextDisabled)
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 2 — Multimedia
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaSection(
    mediaUri:  Uri?,
    mediaType: MediaType,
    onAttach:  () -> Unit,
    onClear:   () -> Unit
) {
    CapsuleCard {
        SectionHeader(
            icon     = Icons.Default.AttachFile,
            title    = stringResource(R.string.capsule_section_media),
            subtitle = stringResource(R.string.capsule_section_media_sub)
        )

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 18.dp))

        if (mediaUri != null) {
            // Vista previa + botón quitar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                when (mediaType) {
                    MediaType.IMAGE -> AsyncImage(
                        model            = mediaUri,
                        contentDescription = stringResource(R.string.desc_photo),
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                    MediaType.VIDEO -> Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp))
                        Text(
                            text      = stringResource(R.string.desc_video),
                            color     = TextSecondary,
                            modifier  = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        )
                    }
                    else -> {}
                }
                // Botón X
                IconButton(
                    onClick  = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(AmoledBlack.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Clear, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Zona punteada para adjuntar
            DashedBorderBox(onClick = onAttach)
        }
    }
}

@Composable
private fun DashedBorderBox(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width      = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f)
            )
            drawRoundRect(
                color       = primary,
                style       = stroke,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text  = stringResource(R.string.capsule_attach_media_hint),
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// § 3 — Condición de Apertura
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TriggerSection(
    triggerType:   TriggerType,
    scheduledDate: Long?,
    onTrigger:     (TriggerType) -> Unit,
    onPickDate:    () -> Unit
) {
    CapsuleCard {
        SectionHeader(
            icon     = Icons.Default.LockClock,
            title    = stringResource(R.string.capsule_section_trigger),
            subtitle = stringResource(R.string.capsule_section_trigger_sub)
        )

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Opción A
            TriggerOption(
                selected    = triggerType == TriggerType.IMMEDIATE_POST_MORTEM,
                label       = stringResource(R.string.trigger_immediate),
                description = stringResource(R.string.trigger_immediate_desc),
                icon        = Icons.Default.Inbox,
                onClick     = { onTrigger(TriggerType.IMMEDIATE_POST_MORTEM) }
            )

            // Opción B
            TriggerOption(
                selected    = triggerType == TriggerType.POST_MORTEM_DATE,
                label       = stringResource(R.string.trigger_scheduled),
                description = stringResource(R.string.trigger_scheduled_desc),
                icon        = Icons.Default.CalendarMonth,
                onClick     = { onTrigger(TriggerType.POST_MORTEM_DATE) }
            )

            // Selector de fecha condicional
            AnimatedVisibility(
                visible = triggerType == TriggerType.POST_MORTEM_DATE,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                val dateLabel = scheduledDate?.let {
                    SimpleDateFormat("dd / MMM / yyyy", Locale.getDefault()).format(Date(it))
                } ?: stringResource(R.string.capsule_no_date_selected)

                OutlinedButton(
                    onClick = onPickDate,
                    shape   = RoundedCornerShape(10.dp),
                    border  = BorderStroke(
                        1.dp,
                        if (scheduledDate == null) ErrorRed.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                    colors  = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (scheduledDate == null) ErrorRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = dateLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerOption(
    selected:    Boolean,
    label:       String,
    description: String,
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    onClick:     () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = MaterialTheme.colorScheme.primary,
                unselectedColor = TextSecondary
            )
        )
        Icon(
            icon, null,
            tint     = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = if (selected) TextPrimary else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = TextSecondary,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CapsuleLoadingOverlay(progress: Float?) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(AmoledBlack.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress          = { progress },
                    modifier          = Modifier.size(64.dp),
                    strokeWidth       = 5.dp,
                    color             = MaterialTheme.colorScheme.primary,
                    trackColor        = SurfaceVariant
                )
                Text(
                    text  = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                CircularProgressIndicator(
                    modifier    = Modifier.size(52.dp),
                    strokeWidth = 4.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text  = stringResource(R.string.capsule_sealing),
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    title:    String,
    subtitle: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.padding(bottom = 18.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlayfairDisplay
                )
            )
            Text(subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}

@Composable
private fun CapsuleCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable
private fun CapsuleMediaSheetItem(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary))
    }
}

@Composable
private fun capsuleFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primaryContainer,
    unfocusedBorderColor    = CardBorder,
    cursorColor             = MaterialTheme.colorScheme.primary,
    focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
    unfocusedContainerColor = Color.Transparent
)
