package com.perenniasystems.lascito

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.perenniasystems.lascito.navigation.LascitoNavGraph
import com.perenniasystems.lascito.navigation.Screen
import com.perenniasystems.lascito.ui.theme.LangState
import com.perenniasystems.lascito.ui.theme.LascitoTheme
import com.perenniasystems.lascito.util.LocaleHelper
import com.perenniasystems.lascito.data.local.OnboardingPreferences
import android.view.WindowManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import com.perenniasystems.lascito.data.repository.ThemeRepository
import com.perenniasystems.lascito.ui.theme.AccentMetal
import com.perenniasystems.lascito.ui.theme.ThemeState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource


/**
 * Single-Activity entry point for Lascito.
 *
 * Language switching is done by re-providing [LocalContext] with a
 * locale-aware wrapper context (via [CompositionLocalProvider] + [LangState]).
 * This triggers instant recomposition of all [stringResource] calls in the
 * subtree — no [Activity.recreate()] needed, no UI flicker.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore the persisted language into LangState so the UI
        // starts in the correct locale straight away.
        LangState.selectedTag = LocaleHelper.getSavedTag(this)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.parseColor("#0D0D0D")
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D0D0D")

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            // ── Instant locale switching ──────────────────────────────────────
            // LangState.selectedTag is a Compose mutableStateOf.
            // Reading it here means: whenever UserSettingsScreen changes the
            // language, this block recomposes, remember(tag) creates a new
            // locale-aware context, and CompositionLocalProvider propagates it
            // down the tree so every stringResource() resolves in the new lang.
            val langTag     = LangState.selectedTag
            val baseContext = LocalContext.current
            val registryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
            val localizedContext = remember(langTag) {
                LocaleHelper.wrapContext(baseContext, langTag)
            }

            // Create DataStore Preferences Repository
            val onboardingPreferences = remember { OnboardingPreferences(baseContext) }
            val hasSeenOnboarding by onboardingPreferences.hasSeenOnboarding.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            
            // Theme setup
            val themeRepository = remember { ThemeRepository(baseContext) }
            val savedThemeColor by themeRepository.themeColorFlow.collectAsStateWithLifecycle(initialValue = "GOLD")
            
            // Map string name to Enum and update ThemeState
            LaunchedEffect(savedThemeColor) {
                val metal = AccentMetal.entries.find { it.name == savedThemeColor } ?: AccentMetal.GOLD
                ThemeState.selectedMetal = metal
            }

            // OpSec Strict Vault Window Flags
            val isStrictVaultEnabled by onboardingPreferences.isStrictVaultEnabled.collectAsStateWithLifecycle(initialValue = false)
            val currentWindow = this@MainActivity.window
            LaunchedEffect(isStrictVaultEnabled) {
                if (isStrictVaultEnabled) {
                    currentWindow.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    currentWindow.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalContext provides localizedContext,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides (registryOwner ?: this)
            ) {
                LascitoTheme {
                    BiometricLockScreen(activity = this@MainActivity) {
                        if (hasSeenOnboarding == null) {
                            // Display a quick blank screen while reading from the DataStore
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color(0xFF0D0D0D))
                            )
                        } else {
                            val navController = rememberNavController()
                            val startDest = if (hasSeenOnboarding == true) Screen.Login.route else Screen.Onboarding.route
                            val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

                            androidx.compose.material3.Scaffold(
                                snackbarHost = { 
                                    androidx.compose.material3.SnackbarHost(snackbarHostState) { data ->
                                        androidx.compose.material3.Snackbar(
                                            modifier = Modifier.padding(bottom = 24.dp),
                                            snackbarData = data,
                                            containerColor = com.perenniasystems.lascito.ui.theme.SurfaceVariant,
                                            contentColor = com.perenniasystems.lascito.ui.theme.TextPrimary,
                                            actionColor = com.perenniasystems.lascito.ui.theme.ThemeState.selectedMetal.color,
                                            actionContentColor = com.perenniasystems.lascito.ui.theme.ThemeState.selectedMetal.color,
                                            dismissActionContentColor = com.perenniasystems.lascito.ui.theme.TextSecondary,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            ) { paddingValues ->
                                Box(modifier = Modifier.padding(paddingValues)) {
                                    LascitoNavGraph(
                                        navController = navController,
                                        startDestination = startDest,
                                        onFinishOnboarding = {
                                            scope.launch {
                                                onboardingPreferences.saveOnboardingState(true)
                                            }
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
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    onAction()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(activity: FragmentActivity, content: @Composable () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isUnlocked by remember { mutableStateOf(false) }
    var lastPauseTime by remember { mutableStateOf(0L) }
    val GRACE_PERIOD_MS = 30_000L

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    lastPauseTime = android.os.SystemClock.elapsedRealtime()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val timeAway = android.os.SystemClock.elapsedRealtime() - lastPauseTime
                    if (timeAway > GRACE_PERIOD_MS) {
                        isUnlocked = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val authenticate = {
        val executor = ContextCompat.getMainExecutor(activity)
            
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isUnlocked = true
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_title))
                .setDescription(activity.getString(R.string.biometric_description))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e("BiometricLock", "Biometric authentication failed", e)
            }
    }

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            authenticate()
        }
    }

    if (isUnlocked) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF0D0D0D)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFE0E0E0), // Plata/Premium
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { authenticate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFC9A96E), // Oro
                        contentColor = androidx.compose.ui.graphics.Color(0xFF0D0D0D)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_unlock_sanctuary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
