package com.perenniasystems.lascito.feature.settings

import com.perenniasystems.lascito.data.repository.PreferencesRepository
import com.perenniasystems.lascito.data.repository.SessionRepository
import com.perenniasystems.lascito.data.error.DataErrors
import com.perenniasystems.lascito.data.error.DataErrorKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException

import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.repository.LegacyRepository
import com.perenniasystems.lascito.core.common.ErrorHandler
import com.perenniasystems.lascito.data.repository.UserRepository

sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    object Success : DeleteAccountState()
    data class Error(@StringRes val messageResId: Int) : DeleteAccountState()
}

sealed class ChangePwdState {
    object Idle : ChangePwdState()
    object Loading : ChangePwdState()
    object Success : ChangePwdState()
    data class Error(@StringRes val messageResId: Int) : ChangePwdState()
}

data class UserSettingsUiState(
    @StringRes val displayNameRes: Int? = R.string.state_loading,
    val displayNameRaw: String? = null,
    
    @StringRes val emailRes: Int? = R.string.state_loading,
    val emailRaw: String? = null,
    
    @StringRes val storageUsedStrRes: Int = R.string.state_calculating,
    @StringRes val storageMaxStrRes: Int = R.string.state_storage_max,
    
    val deleteState: DeleteAccountState = DeleteAccountState.Idle,
    val changePwdState: ChangePwdState = ChangePwdState.Idle
)

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val session: SessionRepository,
    private val userRepository: UserRepository,
    private val legacyRepository: LegacyRepository,
    private val preferences: PreferencesRepository
) : ViewModel() {

    val selectedMetal: StateFlow<com.perenniasystems.lascito.core.designsystem.theme.AccentMetal> = preferences.accentMetal
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.perenniasystems.lascito.core.designsystem.theme.AccentMetal.GOLD)

    private val _uiState = MutableStateFlow(UserSettingsUiState())
    val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    /** Android guarda el idioma por app y recrea la Activity para aplicarlo. */
    fun changeLanguage(language: com.perenniasystems.lascito.data.model.AppLanguage) {
        preferences.setLanguage(language)
    }

    fun changeMetal(metal: com.perenniasystems.lascito.core.designsystem.theme.AccentMetal) {
        viewModelScope.launch { preferences.setAccentMetal(metal) }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = session.currentUser
            if (user != null) {
                var name: String? = user.displayName
                val userEmail = user.email ?: ""

                // Fetch from Firestore if Auth Legacy doesn't have the displayName
                if (name.isNullOrBlank()) {
                    try {
                        withTimeout(15_000L) {
                            name = userRepository.fetchDisplayName(user.uid).getOrThrow()
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        _uiState.value = _uiState.value.copy(displayNameRes = R.string.state_offline, displayNameRaw = null)
                        return@launch
                    } catch (e: IOException) {
                        _uiState.value = _uiState.value.copy(displayNameRes = R.string.state_offline, displayNameRaw = null)
                        return@launch
                    } catch (e: Exception) {
                        // Keep as null if fetch fails, will fallback below
                    }
                }

                _uiState.value = _uiState.value.copy(
                    displayNameRes = if (name.isNullOrBlank()) R.string.state_default_user else null,
                    displayNameRaw = name,
                    emailRes = null,
                    emailRaw = userEmail,
                    storageUsedStrRes = R.string.state_calculating // Placeholder for future phase
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    displayNameRes = R.string.state_unauthenticated,
                    displayNameRaw = null,
                    emailRes = R.string.state_no_email,
                    emailRaw = null,
                    storageUsedStrRes = R.string.state_storage_empty
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val uid = session.currentUser?.uid
            if (uid == null) {
                _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(R.string.error_generic))
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Loading)
            
            try {
                // 1. Obtener todos los perfiles
                val legacies = legacyRepository.getLegacies(uid)
                
                // 2. Ejecutar la cascada profunda para cada perfil
                for (legacy in legacies) {
                    legacyRepository.deleteLegacy(legacy.profileId, legacy.avatarUrl)
                }
                
                // 3. Borrar el registro del usuario (documento Firestore)
                userRepository.deleteUserDocument(uid)
                
                // 4. Eliminar la cuenta en Firebase Auth
                userRepository.deleteAccount()
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Success)
                    }
                    .onFailure { t ->
                        val errorRes = if (DataErrors.classify(t) == DataErrorKind.RECENT_LOGIN_REQUIRED) {
                            R.string.error_recent_login_required
                        } else {
                            ErrorHandler.mapExceptionToResId(t as? Exception ?: Exception(t))
                        }
                        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(errorRes))
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(ErrorHandler.mapExceptionToResId(e)))
            }
        }
    }

    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Idle)
    }

    fun resetChangePwdState() {
        _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Idle)
    }

    fun changePassword(currentPass: String, newPass: String) {
        val user = session.currentUser
        if (user == null || user.email == null) {
            _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_not_authenticated))
            return
        }

        if (newPass.length < 8) {
            _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_password_strength))
            return
        }

        if (currentPass == newPass) {
            _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_password_same_as_current))
            return
        }

        _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Loading)

        viewModelScope.launch {
            try {
                withTimeout(15_000L) {
                    userRepository.changePassword(currentPass, newPass).getOrThrow()
                }
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Success)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_network))
            } catch (e: Exception) {
                val errorRes = when (DataErrors.classify(e)) {
                    DataErrorKind.INVALID_CREDENTIALS -> R.string.error_incorrect_current_password
                    DataErrorKind.WEAK_PASSWORD -> R.string.error_weak_password
                    else -> ErrorHandler.mapExceptionToResId(e)
                }
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(errorRes))
            }
        }
    }

    // ── Bóveda Estricta (stateIn elimina el flicker del toggle) ──────────────

    val isStrictVaultEnabled: StateFlow<Boolean?> = preferences.isStrictVaultEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleStrictVault(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setStrictVaultEnabled(enabled)
        }
    }

    // ── Bloqueo Biométrico Voluntario ─────────────────────────────────────────

    val isBiometricLockEnabled: StateFlow<Boolean?> = preferences.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBiometricLockEnabled(enabled)
        }
    }
}
