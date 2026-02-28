package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.IOException

import androidx.annotation.StringRes
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.repository.ProfileRepository
import com.perenniasystems.lascito.util.ErrorHandler
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.UserRepository
import com.perenniasystems.lascito.data.repository.FirebaseAuthRepository

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

class UserSettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = FirebaseAuthRepository(),
    private val profileRepository: ProfileRepository = FirebaseProfileRepository(),
    private val prefs: com.perenniasystems.lascito.data.local.OnboardingPreferences? = null,
    private val themeRepository: com.perenniasystems.lascito.data.repository.ThemeRepository? = null
) : ViewModel() {

    private val _selectedMetal = MutableStateFlow(com.perenniasystems.lascito.ui.theme.AccentMetal.GOLD)
    val selectedMetal: StateFlow<com.perenniasystems.lascito.ui.theme.AccentMetal> = _selectedMetal.asStateFlow()
    
    private val _selectedLanguage = MutableStateFlow(com.perenniasystems.lascito.data.model.AppLanguage.SPANISH)
    val selectedLanguage: StateFlow<com.perenniasystems.lascito.data.model.AppLanguage> = _selectedLanguage.asStateFlow()

    private val _uiState = MutableStateFlow(UserSettingsUiState())
    val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        
        themeRepository?.let { repo ->
            viewModelScope.launch {
                repo.accentMetalFlow.collect { metal ->
                    _selectedMetal.value = metal
                }
            }
            viewModelScope.launch {
                repo.languageFlow.collect { lang ->
                    _selectedLanguage.value = lang
                }
            }
        }
    }

    fun changeLanguage(language: com.perenniasystems.lascito.data.model.AppLanguage) {
        viewModelScope.launch {
            _selectedLanguage.value = language
            themeRepository?.saveLanguage(language)
            com.perenniasystems.lascito.ui.theme.LangState.selectedTag = language.tag
        }
    }
    
    fun changeLanguageWithDelay(language: com.perenniasystems.lascito.data.model.AppLanguage) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(50)
            changeLanguage(language)
        }
    }
    
    fun changeMetal(metal: com.perenniasystems.lascito.ui.theme.AccentMetal) {
        viewModelScope.launch {
            _selectedMetal.value = metal
            themeRepository?.saveAccentMetal(metal)
            com.perenniasystems.lascito.ui.theme.ThemeState.selectedMetal = metal
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                var name: String? = user.displayName
                val userEmail = user.email ?: ""

                // Fetch from Firestore if Auth Profile doesn't have the displayName
                if (name.isNullOrBlank()) {
                    try {
                        withTimeout(15_000L) {
                            val doc = db.collection("users").document(user.uid).get().await()
                            name = doc.getString("displayName")
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
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(R.string.error_generic))
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Loading)
            
            try {
                // 1. Obtener todos los perfiles
                val profiles = profileRepository.getProfiles(uid)
                
                // 2. Ejecutar la cascada profunda para cada perfil
                for (profile in profiles) {
                    profileRepository.deleteProfile(profile.profileId, profile.avatarUrl)
                }
                
                // 3. Borrar el registro del usuario (documento Firestore)
                userRepository.deleteUserDocument(uid)
                
                // 4. Eliminar la cuenta en Firebase Auth
                userRepository.deleteAccount()
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Success)
                    }
                    .onFailure { t ->
                        val errorRes = if (t is FirebaseAuthRecentLoginRequiredException) {
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
        val user = auth.currentUser
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
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
                withTimeout(15_000L) {
                    user.reauthenticate(credential).await()
                    user.updatePassword(newPass).await()
                }
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Success)
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_incorrect_current_password))
            } catch (e: FirebaseAuthWeakPasswordException) {
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_weak_password))
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(R.string.error_network))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(changePwdState = ChangePwdState.Error(ErrorHandler.mapExceptionToResId(e)))
            }
        }
    }

    val isStrictVaultEnabled = prefs?.isStrictVaultEnabled
    
    fun toggleStrictVault(enabled: Boolean) {
        viewModelScope.launch {
            prefs?.saveStrictVaultState(enabled)
        }
    }
}
