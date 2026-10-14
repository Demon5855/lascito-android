package com.perenniasystems.lascito.ui.app

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.perenniasystems.lascito.navigation.LascitoNavGraph
import com.perenniasystems.lascito.navigation.Screen
import com.perenniasystems.lascito.ui.screens.biometric.BiometricGate
import com.perenniasystems.lascito.ui.theme.AmoledBlack
import com.perenniasystems.lascito.ui.theme.LascitoTheme
import com.perenniasystems.lascito.ui.theme.LocalAccentMetal
import com.perenniasystems.lascito.ui.theme.SurfaceVariant
import com.perenniasystems.lascito.ui.theme.TextPrimary
import com.perenniasystems.lascito.ui.theme.TextSecondary
import com.perenniasystems.lascito.viewmodel.AppViewModel
import com.perenniasystems.lascito.viewmodel.StartDestination
import kotlinx.coroutines.launch

/**
 * Raíz de la UI: tema, Bóveda Estricta, bloqueo biométrico, snackbar y navegación.
 */
@Composable
fun LascitoAppRoot(
    activity: FragmentActivity,
    viewModel: AppViewModel = hiltViewModel()
) {
    val accentMetal by viewModel.accentMetal.collectAsStateWithLifecycle()
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val isStrictVaultEnabled by viewModel.isStrictVaultEnabled.collectAsStateWithLifecycle()

    // OpSec Strict Vault Window Flags
    LaunchedEffect(isStrictVaultEnabled) {
        if (isStrictVaultEnabled) {
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // La navegación se crea aquí, fuera de BiometricGate. El bloqueo muestra el
    // contenido desde ramas distintas (sin sesión / con sesión / desbloqueado); si
    // el NavController viviera dentro, al cambiar de rama o recrear la Activity
    // (cambio de idioma) se perdería la pila y se volvería a la pantalla inicial.
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LascitoTheme(metal = accentMetal) {
        BiometricGate(activity = activity) {
            val start = startDestination
            if (start == null) {
                // Pantalla negra breve mientras se lee el DataStore
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledBlack)
                )
            } else {
                AppScaffold(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    startDestination = start,
                    onOnboardingFinished = viewModel::markOnboardingSeen
                )
            }
        }
    }
}

@Composable
private fun AppScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    startDestination: StartDestination,
    onOnboardingFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(bottom = 24.dp),
                    snackbarData = data,
                    containerColor = SurfaceVariant,
                    contentColor = TextPrimary,
                    actionColor = LocalAccentMetal.current.color,
                    actionContentColor = LocalAccentMetal.current.color,
                    dismissActionContentColor = TextSecondary,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LascitoNavGraph(
                navController = navController,
                startDestination = startDestination.route(),
                onFinishOnboarding = {
                    onOnboardingFinished()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onShowSnackbar = { message, actionLabel, onAction ->
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onAction()
                        }
                    }
                }
            )
        }
    }
}

private fun StartDestination.route(): String = when (this) {
    StartDestination.ONBOARDING -> Screen.Onboarding.route
    StartDestination.LOGIN -> Screen.Login.route
    StartDestination.HOME -> Screen.Dashboard.route
}
