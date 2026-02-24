package com.perenniasystems.lascito.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── Color Scheme (Dark Only) ──────────────────────────────────────────────────
private val LascitoDarkColorScheme = darkColorScheme(
    primary          = MutedGold,
    onPrimary        = AmoledBlack,
    primaryContainer = MutedGoldDim,
    onPrimaryContainer = MutedGoldLight,

    secondary        = MutedGoldDim,
    onSecondary      = TextPrimary,

    background       = AmoledBlack,
    onBackground     = TextPrimary,

    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline          = CardBorder,
    outlineVariant   = MutedGoldDim,

    error            = ErrorRed,
    onError          = AmoledBlack,
)

// ── App Theme ──────────────────────────────────────────────────────────────────
/**
 * LascitoTheme — always applies the dark color scheme.
 * No light theme is provided by design (memorial / AMOLED context).
 */
@Composable
fun LascitoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LascitoDarkColorScheme,
        typography  = LascitoTypography,
        content     = content
    )
}
