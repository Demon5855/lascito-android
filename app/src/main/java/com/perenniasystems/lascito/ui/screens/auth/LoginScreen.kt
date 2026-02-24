package com.perenniasystems.lascito.ui.screens.auth

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.repository.MockUserRepository
import com.perenniasystems.lascito.ui.components.LascitoPrimaryButton
import com.perenniasystems.lascito.ui.components.LascitoTextField
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.util.Validators

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError      by remember { mutableStateOf(false) }

    // Validate format only when the field has been touched (non-empty).
    val emailError = email.isNotBlank() && !Validators.EMAIL_REGEX.matches(email)

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
            Text(
                text  = "✦",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
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
                onValueChange   = { email = it; loginError = false },
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
                onValueChange        = { password = it; loginError = false },
                label                = stringResource(R.string.label_password),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
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

            // Error de credenciales inválidas
            if (loginError) {
                Text(
                    text  = stringResource(R.string.error_login),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── CTA ──────────────────────────────────────────────────────────
            LascitoPrimaryButton(
                text    = stringResource(R.string.btn_login),
                onClick = {
                    if (MockUserRepository.login(email, password)) {
                        onNavigateToDashboard()
                    } else {
                        loginError = true
                    }
                },
                enabled = email.isNotBlank() && !emailError && password.isNotBlank()
            )

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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun LoginScreenPreview() {
    LascitoTheme { LoginScreen() }
}
