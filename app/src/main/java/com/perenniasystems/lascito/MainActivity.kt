package com.perenniasystems.lascito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.perenniasystems.lascito.navigation.LascitoNavGraph
import com.perenniasystems.lascito.ui.theme.LangState
import com.perenniasystems.lascito.ui.theme.LascitoTheme
import com.perenniasystems.lascito.util.LocaleHelper

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
            val localizedContext = remember(langTag) {
                LocaleHelper.wrapContext(baseContext, langTag)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                LascitoTheme {
                    val navController = rememberNavController()
                    LascitoNavGraph(navController = navController)
                }
            }
        }
    }
}
