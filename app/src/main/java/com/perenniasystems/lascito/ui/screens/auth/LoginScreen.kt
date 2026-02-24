package com.perenniasystems.lascito.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.components.LascitoPrimaryButton
import com.perenniasystems.lascito.ui.components.LascitoTextField
import com.perenniasystems.lascito.ui.theme.*

/**
 * Login screen — entry point of the app.
 *
 * Phase 1: No real Firebase Auth. Tapping "Iniciar Sesión" navigates
 * directly to the Dashboard so the team can iterate on UI.
 *
 * @param onNavigateToDashboard Callback invoked on (future) successful login.
 * @param onNavigateToRegister  Callback to open the Register screen.
 */
@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // ── Subtle top gradient veil ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MutedGold.copy(alpha = 0.06f),
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
                style = MaterialTheme.typography.displayLarge.copy(color = MutedGold),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "Lascito",
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text  = "Perennia Systems",
                style = MaterialTheme.typography.bodySmall.copy(
                    letterSpacing = 3.sp,
                    color         = MutedGoldDim
                ),
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── Email ─────────────────────────────────────────────────────────
            LascitoTextField(
                value         = email,
                onValueChange = { email = it },
                label         = stringResource(R.string.label_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Password ──────────────────────────────────────────────────────
            LascitoTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = stringResource(R.string.label_password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── CTA ──────────────────────────────────────────────────────────
            LascitoPrimaryButton(
                text    = stringResource(R.string.btn_login),
                onClick = onNavigateToDashboard
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
    LascitoTheme {
        LoginScreen()
    }
}
