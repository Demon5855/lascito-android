package com.perenniasystems.lascito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier

/**
 * Single-Activity entry point for Lascito.
 *
 * Language switching is done by re-providing [LocalContext] with a
 * locale-aware wrapper context (via [CompositionLocalProvider] + [LangState]).
 * This triggers instant recomposition of all [stringResource] calls in the
 * subtree — no [Activity.recreate()] needed, no UI flicker.
 */
class MainActivity : ComponentActivity() {
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
            val hasSeenOnboarding by onboardingPreferences.hasSeenOnboarding.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides (registryOwner ?: this)
            ) {
                LascitoTheme {
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
                            }
                        )
                    }
                }
            }
        }
    }
}
