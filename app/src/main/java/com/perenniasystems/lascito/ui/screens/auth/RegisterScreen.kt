package com.perenniasystems.lascito.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.ui.components.LascitoPrimaryButton
import com.perenniasystems.lascito.ui.components.LascitoTextField
import com.perenniasystems.lascito.ui.theme.*

/**
 * Register screen — allows new users to create a Lascito account.
 *
 * Phase 1: No real Firebase Auth. "Crear Cuenta" navigates back
 * to Login (future: call Firebase and navigate to Dashboard).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {}
) {
    var fullName        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* empty */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint               = MutedGold
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
                text  = "Crear Cuenta",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text  = "Preserva sus memorias para siempre.",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            LascitoTextField(
                value         = fullName,
                onValueChange = { fullName = it },
                label         = stringResource(R.string.label_full_name)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LascitoTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = stringResource(R.string.label_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LascitoTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = stringResource(R.string.label_password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LascitoTextField(
                value                = confirmPassword,
                onValueChange        = { confirmPassword = it },
                label                = stringResource(R.string.label_confirm_password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError              = confirmPassword.isNotBlank() && confirmPassword != password
            )

            Spacer(modifier = Modifier.height(40.dp))

            LascitoPrimaryButton(
                text    = stringResource(R.string.btn_register),
                onClick = onNavigateBack,  // Phase 2: replace with real Auth call
                enabled = fullName.isNotBlank() && email.isNotBlank()
                        && password.isNotBlank() && password == confirmPassword
            )

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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun RegisterScreenPreview() {
    LascitoTheme { RegisterScreen() }
}
