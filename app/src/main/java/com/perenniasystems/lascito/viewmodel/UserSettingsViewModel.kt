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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.repository.ProfileRepository
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.UserRepository
import com.perenniasystems.lascito.data.repository.FirebaseAuthRepository

sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    object Success : DeleteAccountState()
    data class Error(@StringRes val messageResId: Int) : DeleteAccountState()
}

data class UserSettingsUiState(
    @StringRes val displayNameRes: Int? = R.string.state_loading,
    val displayNameRaw: String? = null,
    
    @StringRes val emailRes: Int? = R.string.state_loading,
    val emailRaw: String? = null,
    
    @StringRes val storageUsedStrRes: Int = R.string.state_calculating,
    @StringRes val storageMaxStrRes: Int = R.string.state_storage_max,
    
    val deleteState: DeleteAccountState = DeleteAccountState.Idle
)

class UserSettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = FirebaseAuthRepository(),
    private val profileRepository: ProfileRepository = FirebaseProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSettingsUiState())
    val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
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
                val result = userRepository.deleteAccount()
                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    if (exception is FirebaseAuthRecentLoginRequiredException) {
                        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(R.string.error_recent_login_required))
                        return@launch
                    }
                    throw exception ?: Exception("Failed to delete Auth account")
                }
                
                _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Success)
            } catch (e: Exception) {
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(R.string.error_recent_login_required))
                } else {
                    _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Error(R.string.error_generic))
                }
            }
        }
    }

    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(deleteState = DeleteAccountState.Idle)
    }
}
