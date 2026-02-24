package com.perenniasystems.lascito.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── App Theme ──────────────────────────────────────────────────────────────────
/**
 * LascitoTheme — always dark. Never a light theme.
 *
 * The [darkColorScheme] is built dynamically from [ThemeState.selectedMetal],
 * so a simple `ThemeState.selectedMetal = AccentMetal.SILVER` anywhere in the
 * app will trigger an instant recomposition with the new accent applied.
 */
@Composable
fun LascitoTheme(
    content: @Composable () -> Unit
) {
    val metal = ThemeState.selectedMetal          // ← reads Compose mutableStateOf

    val colorScheme = darkColorScheme(
        primary            = metal.color,
        onPrimary          = AmoledBlack,
        primaryContainer   = metal.dimColor,
        onPrimaryContainer = MutedGoldLight,

        secondary          = metal.dimColor,
        onSecondary        = TextPrimary,

        background         = AmoledBlack,
        onBackground       = TextPrimary,

        surface            = SurfaceDark,
        onSurface          = TextPrimary,
        surfaceVariant     = SurfaceVariant,
        onSurfaceVariant   = TextSecondary,

        outline            = CardBorder,
        outlineVariant     = metal.dimColor,

        error              = ErrorRed,
        onError            = AmoledBlack,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LascitoTypography,
        content     = content
    )
}

