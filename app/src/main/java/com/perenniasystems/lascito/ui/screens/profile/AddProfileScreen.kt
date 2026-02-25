package com.perenniasystems.lascito.ui.screens.profile

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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.viewmodel.AddProfileUiState
import com.perenniasystems.lascito.viewmodel.AddProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ProfileTypeChoice { HUMAN, PET }

/**
 * VisualTransformation que muestra dígitos crudos (máx. 8) como DD/MM/AAAA.
 * El estado subyacente solo contiene dígitos, por lo que la posición del cursor
 * es siempre correcta, sin el bug de inversión de dígitos.
 *
 * Mapeo de offsets (dígitos → formateado):
 *   0,1 → 0,1          (DD)
 *   2,3 → 3,4  +1      (MM, salta el primer '/')
 *   4..7 → 6..9  +2    (AAAA, salta ambas '/')
 */
private object DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text          // solo dígitos, max 8
        val out = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 2 || i == 4) append('/')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 4 -> offset + 1
                else        -> offset + 2
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset - 1
                else        -> (offset - 2).coerceAtMost(digits.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/** Extrae solo los 8 dígitos de la cadena "dd/MM/yyyy" devuelta por el picker. */
private fun Long.toRawDigits(): String =
    SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date(this))

/**
 * Valida los dígitos crudos de una fecha (máx. 8) en tiempo real.
 * Solo valida un segmento cuando tiene suficientes dígitos para juzgarlo.
 * Devuelve un mensaje de error o null si todo está bien.
 */
private fun validateDateDigits(digits: String): String? {
    if (digits.length < 2) return null
    val day = digits.take(2).toInt()
    if (day !in 1..31) return "Día inválido (01–31)"

    if (digits.length < 4) return null
    val month = digits.drop(2).take(2).toInt()
    if (month !in 1..12) return "Mes inválido (01–12)"

    if (digits.length < 8) return null
    val year  = digits.drop(4).take(4).toInt()
    val maxYear = Calendar.getInstance().get(Calendar.YEAR)
    if (year !in 1900..maxYear) return "Año fuera de rango (1900–$maxYear)"

    // Validar que la fecha no esté en el futuro
    val format = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
    try {
        val parsedDate = format.parse(digits)
        if (parsedDate != null && parsedDate.after(Date())) {
            return "La fecha no puede ser en el futuro"
        }
    } catch (e: Exception) {
        return "Fecha inválida"
    }

    return null   // válido
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    viewModel: AddProfileViewModel,
    onNavigateBack: () -> Unit
) {
    var rawName         by remember { mutableStateOf("") }
    var rawRelationship by remember { mutableStateOf("") }
    var selectedType    by remember { mutableStateOf(ProfileTypeChoice.HUMAN) }
    // Estado = solo dígitos (máx. 8). La transformación visual agrega las '/'.
    var birthDigits  by remember { mutableStateOf("") }
    var passDigits   by remember { mutableStateOf("") }
    var isPresent    by remember { mutableStateOf(true) }

    var showBirthPicker   by remember { mutableStateOf(false) }
    var showPassingPicker by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AddProfileUiState.Loading

    // Handle success navigation
    LaunchedEffect(uiState) {
        if (uiState is AddProfileUiState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }
    val birthPickerState   = rememberDatePickerState()
    val passingPickerState = rememberDatePickerState()

    val birthError = validateDateDigits(birthDigits)
    val passError  = if (!isPresent) validateDateDigits(passDigits) else null
    val canSave    = rawName.isNotBlank() && rawRelationship.isNotBlank()
                     && birthDigits.length == 8 && birthError == null
                     && (isPresent || (passDigits.length == 8 && passError == null))

    // ── Fecha de Nacimiento picker ────────────────────────────────────────────
    if (showBirthPicker) {
        DatePickerDialog(
            onDismissRequest = { showBirthPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthPickerState.selectedDateMillis?.let { birthDigits = it.toRawDigits() }
                    showBirthPicker = false
                }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showBirthPicker = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            colors = pickerColors()
        ) { DatePicker(state = birthPickerState) }
    }

    // ── Fecha de Partida picker ───────────────────────────────────────────────
    if (showPassingPicker) {
        DatePickerDialog(
            onDismissRequest = { showPassingPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    passingPickerState.selectedDateMillis?.let { passDigits = it.toRawDigits() }
                    showPassingPicker = false
                }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showPassingPicker = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            colors = pickerColors()
        ) { DatePicker(state = passingPickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.screen_new_altar), style = MaterialTheme.typography.headlineLarge)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Avatar Picker ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clickable { /* Phase 2: Launch photo picker */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.AddAPhoto,
                        contentDescription = "Subir foto",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(32.dp)
                    )
                    Text(stringResource(R.string.label_photo), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primaryContainer))
                }
            }

            // ── Tipo: Humano / Mascota ────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text     = stringResource(R.string.label_type),
                    style    = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(start = 4.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ProfileTypeChoice.entries.forEachIndexed { index, choice ->
                        SegmentedButton(
                            selected = selectedType == choice,
                            onClick  = { selectedType = choice },
                            shape    = SegmentedButtonDefaults.itemShape(index, ProfileTypeChoice.entries.size),
                            colors   = SegmentedButtonDefaults.colors(
                                activeContainerColor   = MaterialTheme.colorScheme.primary,
                                activeContentColor     = AmoledBlack,
                                inactiveContainerColor = SurfaceDark,
                                inactiveContentColor   = TextSecondary,
                                activeBorderColor      = MaterialTheme.colorScheme.primary,
                                inactiveBorderColor    = CardBorder
                            ),
                            label = {
                                Text(
                                    if (choice == ProfileTypeChoice.HUMAN)
                                        stringResource(R.string.type_human)
                                    else
                                        stringResource(R.string.type_pet),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
            }

            // ── Nombre Completo — capitaliza cada palabra ─────────────────────
            LabeledField(stringResource(R.string.label_full_name_field)) {
                OutlinedTextField(
                    value         = rawName,
                    onValueChange = { rawName = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            if (selectedType == ProfileTypeChoice.HUMAN)
                                stringResource(R.string.placeholder_human_name)
                            else
                                stringResource(R.string.placeholder_pet_name),
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextDisabled)
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlayfairDisplay,
                        color      = TextPrimary
                    ),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Next
                    ),
                    colors = diaryFieldColors(),
                    shape  = RoundedCornerShape(10.dp)
                )
            }

            // ── Parentesco / Vínculo ──────────────────────────────────────────
            LabeledField(stringResource(R.string.label_relationship)) {
                OutlinedTextField(
                    value         = rawRelationship,
                    onValueChange = { rawRelationship = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            if (selectedType == ProfileTypeChoice.HUMAN)
                                stringResource(R.string.placeholder_human_rel)
                            else
                                stringResource(R.string.placeholder_pet_rel),
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextDisabled)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Next
                    ),
                    colors = diaryFieldColors(),
                    shape  = RoundedCornerShape(10.dp)
                )
            }

            // ── Fecha de Nacimiento ───────────────────────────────────────────
            LabeledField(stringResource(R.string.label_birthdate)) {
                OutlinedTextField(
                    value         = birthDigits,
                    onValueChange = { birthDigits = it.filter(Char::isDigit).take(8) },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(stringResource(R.string.hint_date_format),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled))
                    },
                    textStyle             = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    singleLine            = true,
                    isError               = birthError != null,
                    keyboardOptions       = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation  = DateVisualTransformation,
                    trailingIcon = {
                        IconButton(onClick = { showBirthPicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Seleccionar fecha",
                                tint     = if (birthError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = if (birthError != null) errorFieldColors() else diaryFieldColors(),
                    shape  = RoundedCornerShape(10.dp)
                )
                if (birthError != null) {
                    Text(
                        text     = birthError,
                        color    = ErrorRed,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            // ── Switch "Aún está con nosotros" ────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (isPresent) stringResource(R.string.label_still_with_us)
                        else           stringResource(R.string.label_departed),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Text(
                        if (isPresent) stringResource(R.string.label_present)
                        else           stringResource(R.string.label_passing_date_hint),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                Switch(
                    checked         = isPresent,
                    onCheckedChange = { isPresent = it; if (it) passDigits = "" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor    = AmoledBlack,
                        checkedTrackColor    = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor  = TextSecondary,
                        uncheckedTrackColor  = SurfaceDark,
                        uncheckedBorderColor = CardBorder
                    )
                )
            }

            // ── Fecha de Partida (solo si !isPresent) ────────────────────────
            if (!isPresent) {
                LabeledField(stringResource(R.string.label_passing_date)) {
                    OutlinedTextField(
                        value         = passDigits,
                        onValueChange = { passDigits = it.filter(Char::isDigit).take(8) },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text(stringResource(R.string.hint_date_format),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled))
                        },
                        textStyle             = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine            = true,
                        isError               = passError != null,
                        keyboardOptions       = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation  = DateVisualTransformation,
                        trailingIcon = {
                            IconButton(onClick = { showPassingPicker = true }) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Seleccionar fecha",
                                    tint     = if (passError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = if (passError != null) errorFieldColors() else diaryFieldColors(),
                        shape  = RoundedCornerShape(10.dp)
                    )
                    if (passError != null) {
                        Text(
                            text     = passError,
                            color    = ErrorRed,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show error if any (Delegated to ErrorHandler + LascitoAlertDialog)
            if (uiState is AddProfileUiState.Error) {
                val errState = uiState as AddProfileUiState.Error
                LascitoAlertDialog(
                    messageRes = errState.messageResId,
                    onDismiss  = { viewModel.resetState() }
                )
            }

            // ── Levantar Altar Button ─────────────────────────────────────────
            Button(
                onClick  = {
                    val format = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
                    val birthMillis = try { format.parse(birthDigits)?.time ?: 0L } catch (e: Exception) { 0L }
                    val passMillis = if (isPresent || passDigits.isBlank()) null else {
                        try { format.parse(passDigits)?.time } catch (e: Exception) { null }
                    }

                    val pType = if (selectedType == ProfileTypeChoice.HUMAN) ProfileType.HUMAN else ProfileType.PET
                    viewModel.saveProfile(
                        fullName = rawName,
                        profileType = pType,
                        relationship = rawRelationship,
                        birthDate = birthMillis,
                        passingDate = passMillis
                    )
                },
                enabled  = canSave && !isLoading,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = AmoledBlack,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor   = TextDisabled
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AmoledBlack,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.btn_create_altar), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

@Composable
private fun diaryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primaryContainer,
    unfocusedBorderColor    = CardBorder,
    cursorColor             = MaterialTheme.colorScheme.primary,
    focusedContainerColor   = SurfaceDark.copy(alpha = 0.4f),
    unfocusedContainerColor = Color.Transparent
)

@Composable
private fun errorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = ErrorRed,
    unfocusedBorderColor    = ErrorRed.copy(alpha = 0.6f),
    cursorColor             = ErrorRed,
    focusedContainerColor   = ErrorRed.copy(alpha = 0.05f),
    unfocusedContainerColor = ErrorRed.copy(alpha = 0.03f)
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pickerColors() = DatePickerDefaults.colors(
    containerColor             = SurfaceDark,
    titleContentColor          = TextPrimary,
    headlineContentColor       = TextPrimary,
    weekdayContentColor        = TextSecondary,
    subheadContentColor        = TextSecondary,
    navigationContentColor     = MaterialTheme.colorScheme.primary,
    yearContentColor           = TextPrimary,
    currentYearContentColor    = MaterialTheme.colorScheme.primary,
    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
    selectedYearContentColor   = AmoledBlack,
    dayContentColor            = TextPrimary,
    selectedDayContainerColor  = MaterialTheme.colorScheme.primary,
    selectedDayContentColor    = AmoledBlack,
    todayContentColor          = MaterialTheme.colorScheme.primary,
    todayDateBorderColor       = MaterialTheme.colorScheme.primary
)

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AddProfileScreenPreview() {
    LascitoTheme { AddProfileScreen(viewModel = AddProfileViewModel()) {} }
}
