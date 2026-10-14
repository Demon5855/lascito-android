package com.perenniasystems.lascito.ui.screens.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.components.LascitoAlertDialog
import com.perenniasystems.lascito.ui.components.LascitoPrimaryButton
import com.perenniasystems.lascito.ui.components.LascitoTextField
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.util.ValidationRules
import com.perenniasystems.lascito.viewmodel.AuthUiState
import com.perenniasystems.lascito.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel:  AuthViewModel,
    onNavigateBack: () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var fullName        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var termsAccepted   by remember { mutableStateOf(false) }

    val emailError     = email.isNotBlank()    && ValidationRules.validateEmail(email) != null
    val passwordError   = password.isNotBlank() && ValidationRules.validatePassword(password) != null
    val confirmError    = confirmPassword.isNotBlank() && confirmPassword != password

    val canSubmit = fullName.isNotBlank()
            && email.isNotBlank() && !emailError
            && password.isNotBlank() && !passwordError
            && confirmPassword == password
            && termsAccepted

    // ── Accent color for styled links ─────────────────────────────────────────
    val accentColor = MaterialTheme.colorScheme.primary
    val termsText = buildAnnotatedString {
        append(stringResource(R.string.terms_prefix))
        withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)) {
            append(stringResource(R.string.terms_tos))
        }
        append(stringResource(R.string.terms_connector))
        withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)) {
            append(stringResource(R.string.terms_privacy))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* empty */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text  = stringResource(R.string.screen_register),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text      = stringResource(R.string.subtitle_register),
                style     = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Nombre completo ───────────────────────────────────────────────
            LascitoTextField(
                value         = fullName,
                onValueChange = { fullName = it },
                label         = stringResource(R.string.label_full_name)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Email ─────────────────────────────────────────────────────────
            LascitoTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = stringResource(R.string.label_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError         = emailError
            )
            if (emailError) {
                Text(
                    text     = stringResource(R.string.error_invalid_email),
                    color    = ErrorRed,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── Contraseña ────────────────────────────────────────────────────
            LascitoTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = stringResource(R.string.label_password),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError         = passwordError,
                trailingIcon    = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector        = if (passwordVisible)
                                Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.desc_hide_password
                                else                 R.string.desc_show_password
                            ),
                            tint = TextSecondary
                        )
                    }
                }
            )
            if (passwordError) {
                Text(
                    text     = stringResource(R.string.error_password_strength),
                    color    = ErrorRed,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── Confirmar contraseña ──────────────────────────────────────────
            LascitoTextField(
                value                = confirmPassword,
                onValueChange        = { confirmPassword = it },
                label                = stringResource(R.string.label_confirm_password),
                visualTransformation = if (confirmVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError         = confirmError,
                trailingIcon    = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector        = if (confirmVisible)
                                Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (confirmVisible) R.string.desc_hide_password
                                else               R.string.desc_show_password
                            ),
                            tint = if (confirmError) ErrorRed else TextSecondary
                        )
                    }
                }
            )
            if (confirmError) {
                Text(
                    text     = stringResource(R.string.error_passwords_no_match),
                    color    = ErrorRed,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Consentimiento de Términos ─────────────────────────────────────
            // The Checkbox turns gold when checked; "Términos de Servicio" and
            // "Política de Privacidad" are underlined + colored to look like links.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { termsAccepted = !termsAccepted }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked         = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = MaterialTheme.colorScheme.primary,
                        uncheckedColor = CardBorder,
                        checkmarkColor = AmoledBlack
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = termsText,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Crear Cuenta — disabled (gray) until form + terms are valid ───
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            val isLoading = authState is AuthUiState.Loading

            // Show Firebase error if any
            if (authState is AuthUiState.Error) {
                val errorState = authState as AuthUiState.Error
                LascitoAlertDialog(
                    messageRes = errorState.messageResId,
                    onDismiss = { authViewModel.resetState() },
                    formatArgs = errorState.formatArgs
                )
            }

            LascitoPrimaryButton(
                text    = if (isLoading) stringResource(R.string.btn_loading)
                          else           stringResource(R.string.btn_register),
                onClick = { authViewModel.register(fullName, email, password) },
                enabled = !isLoading && canSubmit
            )

            // Navigate on success
            LaunchedEffect(authState) {
                if (authState is AuthUiState.Success) {
                    onShowSnackbar(context.getString(R.string.toast_verification_sent), null) {}
                    authViewModel.resetState()
                    onNavigateBack()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text      = stringResource(R.string.link_have_account),
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier  = Modifier.clickable { onNavigateBack() }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
