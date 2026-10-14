package com.perenniasystems.lascito.ui.theme

import androidx.compose.runtime.compositionLocalOf

// ── Accent metal options ──────────────────────────────────────────────────────
// Moved here so both Theme.kt and UserSettingsScreen can reference the same type.

enum class AccentMetal(
    val labelRes: Int,
    val color:    androidx.compose.ui.graphics.Color,
    val dimColor: androidx.compose.ui.graphics.Color
) {
    GOLD  (com.perenniasystems.lascito.R.string.accent_gold,   MutedGold,    MutedGoldDim),
    SILVER(com.perenniasystems.lascito.R.string.accent_silver, SilverAccent, SilverAccentDim),
    BRONZE(com.perenniasystems.lascito.R.string.accent_bronze, BronzeAccent, BronzeAccentDim)
}

/** Color de acento activo, provisto por [LascitoTheme]. */
val LocalAccentMetal = compositionLocalOf { AccentMetal.GOLD }
