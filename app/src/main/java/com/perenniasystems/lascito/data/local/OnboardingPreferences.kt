package com.perenniasystems.lascito.data.local

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        private val IS_STRICT_VAULT_ENABLED = booleanPreferencesKey("is_strict_vault_enabled")
        private val IS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        private val HAS_SEEN_BIOMETRIC_PROMO = booleanPreferencesKey("has_seen_biometric_promo")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING] ?: false
        }

    suspend fun saveOnboardingState(hasSeen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = hasSeen
        }
    }

    val isStrictVaultEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_STRICT_VAULT_ENABLED] ?: false
        }

    suspend fun saveStrictVaultState(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_STRICT_VAULT_ENABLED] = isEnabled
        }
    }

    // ── Bloqueo Biométrico Voluntario ─────────────────────────────────────────

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_BIOMETRIC_LOCK_ENABLED] ?: false
        }

    suspend fun saveBiometricLockState(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_BIOMETRIC_LOCK_ENABLED] = isEnabled
        }
    }

    // ── Promo Biométrico (one-shot) ───────────────────────────────────────────

    val hasSeenBiometricPromo: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_BIOMETRIC_PROMO] ?: false
        }

    suspend fun saveBiometricPromoSeen() {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_BIOMETRIC_PROMO] = true
        }
    }
}
