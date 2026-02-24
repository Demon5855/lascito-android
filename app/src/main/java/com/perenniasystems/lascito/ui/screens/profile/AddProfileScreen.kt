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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.ui.theme.*
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

    return null   // válido
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf(ProfileTypeChoice.HUMAN) }
    var fullName     by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    // Estado = solo dígitos (máx. 8). La transformación visual agrega las '/'.
    var birthDigits  by remember { mutableStateOf("") }
    var passDigits   by remember { mutableStateOf("") }
    var isPresent    by remember { mutableStateOf(true) }

    var showBirthPicker   by remember { mutableStateOf(false) }
    var showPassingPicker by remember { mutableStateOf(false) }
    val birthPickerState   = rememberDatePickerState()
    val passingPickerState = rememberDatePickerState()

    val birthError = validateDateDigits(birthDigits)
    val passError  = if (!isPresent) validateDateDigits(passDigits) else null
    val canSave    = fullName.isNotBlank() && relationship.isNotBlank()
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
                }) { Text("Aceptar", color = MutedGold) }
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
                }) { Text("Aceptar", color = MutedGold) }
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
                        Text("Crear Altar", style = MaterialTheme.typography.headlineLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint               = MutedGold
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
                    .border(1.5.dp, MutedGoldDim, CircleShape)
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
                        tint               = MutedGold,
                        modifier           = Modifier.size(32.dp)
                    )
                    Text("Foto", style = MaterialTheme.typography.labelSmall.copy(color = MutedGoldDim))
                }
            }

            // ── Tipo: Humano / Mascota ────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text     = "Tipo",
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
                                activeContainerColor   = MutedGold,
                                activeContentColor     = AmoledBlack,
                                inactiveContainerColor = SurfaceDark,
                                inactiveContentColor   = TextSecondary,
                                activeBorderColor      = MutedGold,
                                inactiveBorderColor    = CardBorder
                            ),
                            label = {
                                Text(
                                    if (choice == ProfileTypeChoice.HUMAN) "Humano" else "Mascota",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
            }

            // ── Nombre Completo — capitaliza cada palabra ─────────────────────
            LabeledField("Nombre Completo") {
                OutlinedTextField(
                    value         = fullName,
                    onValueChange = { fullName = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            if (selectedType == ProfileTypeChoice.HUMAN) "Elena Ríos Marchetti" else "Mochi",
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
            LabeledField("Parentesco / Vínculo") {
                OutlinedTextField(
                    value         = relationship,
                    onValueChange = { relationship = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            if (selectedType == ProfileTypeChoice.HUMAN) "Abuela, Padre, Amigo…" else "Compañero fiel",
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
            LabeledField("Fecha de Nacimiento") {
                OutlinedTextField(
                    value         = birthDigits,
                    onValueChange = { birthDigits = it.filter(Char::isDigit).take(8) },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text("DD/MM/AAAA",
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
                                tint     = if (birthError != null) ErrorRed else MutedGold,
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
                        if (isPresent) "Aún está con nosotros" else "Ha partido",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Text(
                        if (isPresent) "Presente" else "Ingresa la fecha de partida",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                Switch(
                    checked         = isPresent,
                    onCheckedChange = { isPresent = it; if (it) passDigits = "" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor    = AmoledBlack,
                        checkedTrackColor    = MutedGold,
                        uncheckedThumbColor  = TextSecondary,
                        uncheckedTrackColor  = SurfaceDark,
                        uncheckedBorderColor = CardBorder
                    )
                )
            }

            // ── Fecha de Partida (solo si !isPresent) ────────────────────────
            if (!isPresent) {
                LabeledField("Fecha de Partida") {
                    OutlinedTextField(
                        value         = passDigits,
                        onValueChange = { passDigits = it.filter(Char::isDigit).take(8) },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text("DD/MM/AAAA",
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
                                    tint     = if (passError != null) ErrorRed else MutedGold,
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

            // ── Levantar Altar Button ─────────────────────────────────────────
            Button(
                onClick  = { onNavigateBack() },   // Phase 2: persist to Firestore
                enabled  = canSave,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MutedGold,
                    contentColor           = AmoledBlack,
                    disabledContainerColor = MutedGoldDim,
                    disabledContentColor   = TextDisabled
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Levantar Altar", style = MaterialTheme.typography.labelLarge)
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
    focusedBorderColor      = MutedGoldDim,
    unfocusedBorderColor    = CardBorder,
    cursorColor             = MutedGold,
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
    navigationContentColor     = MutedGold,
    yearContentColor           = TextPrimary,
    currentYearContentColor    = MutedGold,
    selectedYearContainerColor = MutedGold,
    selectedYearContentColor   = AmoledBlack,
    dayContentColor            = TextPrimary,
    selectedDayContainerColor  = MutedGold,
    selectedDayContentColor    = AmoledBlack,
    todayContentColor          = MutedGold,
    todayDateBorderColor       = MutedGold
)

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AddProfileScreenPreview() {
    LascitoTheme { AddProfileScreen() }
}
