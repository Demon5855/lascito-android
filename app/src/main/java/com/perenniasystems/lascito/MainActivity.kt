package com.perenniasystems.lascito

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.perenniasystems.lascito.data.repository.PreferencesRepository
import com.perenniasystems.lascito.app.LascitoAppRoot
import com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-Activity entry point for Lascito.
 *
 * El idioma lo aplica Android como idioma por app: al cambiarlo, la Activity se
 * recrea con la nueva configuración. La UI vive en [LascitoAppRoot].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferences: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bóveda Estricta: FLAG_SECURE lo antes posible, antes del primer frame.
        lifecycleScope.launch {
            if (preferences.isStrictVaultEnabled.first()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, true)

        @Suppress("DEPRECATION")
        window.statusBarColor = AmoledBlack.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = AmoledBlack.toArgb()

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            LascitoAppRoot(activity = this)
        }
    }
}
