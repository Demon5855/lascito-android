package com.perenniasystems.lascito.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.repository.PreferencesRepository
import com.perenniasystems.lascito.data.repository.SessionRepository
import com.perenniasystems.lascito.core.designsystem.theme.AccentMetal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination { ONBOARDING, LOGIN, HOME }

/** Estado de la raíz de la app: pantalla inicial, color de acento y Bóveda Estricta. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val session: SessionRepository
) : ViewModel() {

    val accentMetal: StateFlow<AccentMetal> = preferences.accentMetal
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentMetal.GOLD)

    val isStrictVaultEnabled: StateFlow<Boolean> = preferences.isStrictVaultEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Se decide una sola vez al arrancar (null mientras se lee el DataStore).
     * Cambiarla después reconstruiría el NavHost en mitad de la navegación.
     */
    val startDestination: StateFlow<StartDestination?> = flow {
        val hasSeenOnboarding = preferences.hasSeenOnboarding.first()
        emit(
            when {
                !hasSeenOnboarding -> StartDestination.ONBOARDING
                session.currentUser != null -> StartDestination.HOME
                else -> StartDestination.LOGIN
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markOnboardingSeen() {
        viewModelScope.launch { preferences.setOnboardingSeen() }
    }
}
