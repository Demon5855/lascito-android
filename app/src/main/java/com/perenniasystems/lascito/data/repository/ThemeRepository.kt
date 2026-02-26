package com.perenniasystems.lascito.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeRepository(private val context: Context) {

    private val THEME_COLOR_KEY = stringPreferencesKey("selected_theme_color")

    val themeColorFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Return GOLD as default if nothing is saved
            preferences[THEME_COLOR_KEY] ?: "GOLD"
        }

    suspend fun saveThemeColor(colorName: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_COLOR_KEY] = colorName
        }
    }
}
