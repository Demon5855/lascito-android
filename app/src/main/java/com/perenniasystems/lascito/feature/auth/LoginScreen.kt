package com.perenniasystems.lascito.feature.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.core.designsystem.component.LascitoAlertDialog
import com.perenniasystems.lascito.core.designsystem.component.LascitoPrimaryButton
import com.perenniasystems.lascito.core.designsystem.component.LascitoTextField
import com.perenniasystems.lascito.core.designsystem.theme.*
import com.perenniasystems.lascito.core.common.ValidationRules
import com.perenniasystems.lascito.feature.auth.AuthUiState
import com.perenniasystems.lascito.feature.auth.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel:         AuthViewModel,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToRegister:  () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = authState is AuthUiState.Loading

    // Validate format only when the field has been touched (non-empty).
    val emailError = email.isNotBlank() && ValidationRules.validateEmail(email) != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // ── Subtle gradient veil aligned to accent color ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // ── Logotype ──────────────────────────────────────────────────────
            Icon(
                painter = painterResource(R.drawable.ic_perennia_leaf_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "Lascito",
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text  = stringResource(R.string.company_name),
                style = MaterialTheme.typography.bodySmall.copy(
                    letterSpacing = 3.sp,
                    color         = MaterialTheme.colorScheme.primaryContainer
                ),
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── Email ─────────────────────────────────────────────────────────
            LascitoTextField(
                value           = email,
                onValueChange   = { email = it; if (authState is AuthUiState.Error) authViewModel.resetState() },
                label           = stringResource(R.string.label_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError         = emailError
            )
            if (emailError) {
                Text(
                    text  = stringResource(R.string.error_invalid_email),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Password ──────────────────────────────────────────────────────
            LascitoTextField(
                value                = password,
                onValueChange        = { password = it; if (authState is AuthUiState.Error) authViewModel.resetState() },
                label                = stringResource(R.string.label_password),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = androidx.compose.ui.text.input.ImeAction.Done
                ),
                keyboardActions      = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        if (!isLoading && email.isNotBlank() && !emailError && password.isNotBlank()) {
                            authViewModel.login(email, password)
                        }
                    }
                ),
                trailingIcon         = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Filled.VisibilityOff
                            else
                                Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.desc_hide_password
                                else                 R.string.desc_show_password
                            ),
                            tint = TextSecondary
                        )
                    }
                }
            )

            // ── Forgot Password Compact ───────────────────────────────────────
            androidx.compose.material3.TextButton(
                onClick = {
                    if (email.isBlank()) {
                        onShowSnackbar(context.getString(R.string.error_enter_email_first) + "\n" + context.getString(R.string.hint_forgot_email), null) {}
                    } else {
                        authViewModel.resetPassword(email)
                    }
                },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_forgot_password),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Error de credenciales inválidas o de autenticación
            if (authState is AuthUiState.Error) {
                val errorState = authState as AuthUiState.Error
                LascitoAlertDialog(
                    messageRes = errorState.messageResId,
                    onDismiss = { authViewModel.resetState() },
                    formatArgs = errorState.formatArgs
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // ── CTA ──────────────────────────────────────────────────────────
            LascitoPrimaryButton(
                text    = if (isLoading) stringResource(R.string.btn_loading)
                          else           stringResource(R.string.btn_login),
                onClick = { authViewModel.login(email, password) },
                enabled = !isLoading && email.isNotBlank() && !emailError && password.isNotBlank()
            )

            // Navigate or show snackbar on success
            LaunchedEffect(authState) {
                if (authState is AuthUiState.Success) {
                    onNavigateToDashboard()
                } else if (authState is AuthUiState.ResetEmailSent) {
                    onShowSnackbar(context.getString(R.string.msg_reset_email_sent, email), null) {}
                    authViewModel.resetState()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Register link ─────────────────────────────────────────────────
            Text(
                text      = stringResource(R.string.link_no_account),
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier  = Modifier.clickable { onNavigateToRegister() }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
