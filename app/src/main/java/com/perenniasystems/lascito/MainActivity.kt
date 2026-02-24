package com.perenniasystems.lascito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.perenniasystems.lascito.navigation.LascitoNavGraph
import com.perenniasystems.lascito.ui.theme.LascitoTheme

/**
 * Single-Activity entry point for Lascito.
 *
 * We deliberately do NOT call enableEdgeToEdge() here.
 * Instead, we manually force the system bars to AMOLED black so the
 * dark theme is consistent from the very first frame, and we let each
 * screen's Scaffold handle its own insets without any parent interference.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the window fitting system windows (default behaviour —
        // content does NOT draw behind status/nav bars).
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Force both bars to AMOLED black with light-coloured icons/text.
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.parseColor("#0D0D0D")
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D0D0D")

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = false   // white icons on dark bar
            isAppearanceLightNavigationBars = false
        }

        setContent {
            LascitoTheme {
                val navController = rememberNavController()
                LascitoNavGraph(navController = navController)
            }
        }
    }
}
