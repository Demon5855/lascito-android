package com.perenniasystems.lascito.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeRepository(private val context: Context) {

    private val THEME_COLOR_KEY = stringPreferencesKey("selected_theme_color")
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    val themeColorFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_COLOR_KEY] ?: "GOLD"
        }

    val accentMetalFlow: Flow<com.perenniasystems.lascito.ui.theme.AccentMetal> = context.dataStore.data.map { prefs ->
        val metalName = prefs[THEME_COLOR_KEY] ?: "GOLD"
        com.perenniasystems.lascito.ui.theme.AccentMetal.entries.find { it.name == metalName } ?: com.perenniasystems.lascito.ui.theme.AccentMetal.GOLD
    }
    
    val languageFlow: Flow<com.perenniasystems.lascito.data.model.AppLanguage> = context.dataStore.data.map { prefs ->
        val langTag = prefs[LANGUAGE_KEY] ?: "es"
        com.perenniasystems.lascito.data.model.AppLanguage.entries.find { it.tag == langTag } ?: com.perenniasystems.lascito.data.model.AppLanguage.SPANISH
    }

    suspend fun saveThemeColor(colorName: String) {
        withTimeout(5_000L) {
            context.dataStore.edit { preferences ->
                preferences[THEME_COLOR_KEY] = colorName
            }
        }
    }

    suspend fun saveAccentMetal(metal: com.perenniasystems.lascito.ui.theme.AccentMetal) {
        withTimeout(5_000L) {
            context.dataStore.edit { prefs ->
                prefs[THEME_COLOR_KEY] = metal.name
            }
        }
    }
    
    suspend fun saveLanguage(language: com.perenniasystems.lascito.data.model.AppLanguage) {
        withTimeout(5_000L) {
            context.dataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = language.tag
            }
        }
    }
}
