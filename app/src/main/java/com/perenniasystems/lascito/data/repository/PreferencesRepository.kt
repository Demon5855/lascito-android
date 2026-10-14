package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.AppLanguage
import com.perenniasystems.lascito.ui.theme.AccentMetal
import kotlinx.coroutines.flow.Flow

/**
 * Preferencias locales del dispositivo (un único DataStore).
 *
 * El idioma no se guarda aquí: lo persiste Android como idioma por app
 * (AppCompatDelegate.setApplicationLocales) y se ve en los Ajustes del sistema.
 */
interface PreferencesRepository {

    val accentMetal: Flow<AccentMetal>
    val hasSeenOnboarding: Flow<Boolean>
    val isStrictVaultEnabled: Flow<Boolean>
    val isBiometricLockEnabled: Flow<Boolean>
    val hasSeenBiometricPromo: Flow<Boolean>

    suspend fun setAccentMetal(metal: AccentMetal)
    suspend fun setOnboardingSeen()
    suspend fun setStrictVaultEnabled(enabled: Boolean)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setBiometricPromoSeen()

    /** Cambia el idioma de la app. Android recrea la Activity para aplicarlo. */
    fun setLanguage(language: AppLanguage)
}
