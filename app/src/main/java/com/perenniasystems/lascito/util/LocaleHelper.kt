package com.perenniasystems.lascito.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Helpers for persisting and applying the user's chosen language.
 *
 * Strategy: save the BCP-47 language tag to SharedPreferences.
 * [wrapContext] builds a locale-aware Context; MainActivity provides this via
 * [CompositionLocalProvider] so every [stringResource] call in Compose
 * resolves the correct language instantly — no Activity.recreate() needed.
 */
object LocaleHelper {

    private const val PREF_NAME = "lascito_prefs"
    private const val PREF_LANG = "language_tag"

    /** Returns the persisted language tag, defaulting to "es" (Spanish). */
    fun getSavedTag(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANG, "es") ?: "es"

    /** Persists the chosen language tag to SharedPreferences. */
    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANG, tag)
            .apply()
    }

    /**
     * Returns a new [Context] whose resources are configured for [tag].
     * Call inside a `remember(tag)` block in a composable so the context is
     * recreated only when the tag actually changes.
     *
     * Uses the modern [Context.createConfigurationContext] API (no deprecation).
     */
    fun wrapContext(base: Context, tag: String): Context {
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

