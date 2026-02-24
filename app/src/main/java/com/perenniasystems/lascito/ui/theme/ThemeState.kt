package com.perenniasystems.lascito.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

/**
 * Singleton that holds the currently active accent metal for the entire app.
 * Because [selectedMetal] is backed by Compose [mutableStateOf], any composable
 * that reads it will automatically recompose when the value changes — including
 * [LascitoTheme], which builds a dynamic [darkColorScheme] from it.
 */
object ThemeState {
    var selectedMetal by mutableStateOf(AccentMetal.GOLD)
}

/**
 * Singleton that holds the active BCP-47 language tag.
 * Changing [selectedTag] causes [MainActivity] to re-provide a locale-aware
 * [android.content.Context] via [CompositionLocalProvider], so every
 * [stringResource] call re-resolves in the new language instantly —
 * no [Activity.recreate()] needed.
 */
object LangState {
    var selectedTag by mutableStateOf("es")
}
