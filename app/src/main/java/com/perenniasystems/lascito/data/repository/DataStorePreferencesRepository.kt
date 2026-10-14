package com.perenniasystems.lascito.data.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.perenniasystems.lascito.data.model.AppLanguage
import com.perenniasystems.lascito.core.designsystem.theme.AccentMetal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lascitoPreferences: DataStore<Preferences> by preferencesDataStore(name = "lascito_preferences")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val ACCENT_METAL = stringPreferencesKey("accent_metal")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val STRICT_VAULT_ENABLED = booleanPreferencesKey("is_strict_vault_enabled")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        val HAS_SEEN_BIOMETRIC_PROMO = booleanPreferencesKey("has_seen_biometric_promo")
    }

    private val store get() = context.lascitoPreferences

    override val accentMetal: Flow<AccentMetal> = store.data.map { prefs ->
        AccentMetal.entries.find { it.name == prefs[Keys.ACCENT_METAL] } ?: AccentMetal.GOLD
    }

    override val hasSeenOnboarding: Flow<Boolean> = store.data.map { it[Keys.HAS_SEEN_ONBOARDING] ?: false }

    override val isStrictVaultEnabled: Flow<Boolean> = store.data.map { it[Keys.STRICT_VAULT_ENABLED] ?: false }

    override val isBiometricLockEnabled: Flow<Boolean> = store.data.map { it[Keys.BIOMETRIC_LOCK_ENABLED] ?: false }

    override val hasSeenBiometricPromo: Flow<Boolean> = store.data.map { it[Keys.HAS_SEEN_BIOMETRIC_PROMO] ?: false }

    override suspend fun setAccentMetal(metal: AccentMetal) {
        store.edit { it[Keys.ACCENT_METAL] = metal.name }
    }

    override suspend fun setOnboardingSeen() {
        store.edit { it[Keys.HAS_SEEN_ONBOARDING] = true }
    }

    override suspend fun setStrictVaultEnabled(enabled: Boolean) {
        store.edit { it[Keys.STRICT_VAULT_ENABLED] = enabled }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        store.edit { it[Keys.BIOMETRIC_LOCK_ENABLED] = enabled }
    }

    override suspend fun setBiometricPromoSeen() {
        store.edit { it[Keys.HAS_SEEN_BIOMETRIC_PROMO] = true }
    }

    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
