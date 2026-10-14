package com.perenniasystems.lascito.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── App Theme ──────────────────────────────────────────────────────────────────
/**
 * LascitoTheme — always dark. Never a light theme.
 *
 * The [darkColorScheme] is built from [metal], which the app root reads from
 * the preferences; the same value is exposed to the tree as [LocalAccentMetal].
 */
@Composable
fun LascitoTheme(
    metal: AccentMetal = AccentMetal.GOLD,
    content: @Composable () -> Unit
) {
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

    CompositionLocalProvider(LocalAccentMetal provides metal) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = LascitoTypography,
            content     = content
        )
    }
}

