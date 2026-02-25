package com.perenniasystems.lascito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepositoryImpl
import com.perenniasystems.lascito.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.util.ErrorHandler
import java.io.IOException
import java.util.UUID

sealed interface AddProfileUiState {
    data object Idle : AddProfileUiState
    data object Loading : AddProfileUiState
    data class Loaded(val profile: Profile) : AddProfileUiState
    data object Success : AddProfileUiState
    data class Error(@StringRes val messageResId: Int) : AddProfileUiState
}

class AddProfileViewModel(
    private val repository: ProfileRepository = FirebaseProfileRepository(),
    private val storage: FirebaseStorageRepository = FirebaseStorageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddProfileUiState>(AddProfileUiState.Idle)
    val uiState: StateFlow<AddProfileUiState> = _uiState.asStateFlow()

    private var currentProfileId: String? = null
    private var currentAvatarUrl: String? = null

    fun resetState() {
        _uiState.value = AddProfileUiState.Idle
        currentProfileId = null
        currentAvatarUrl = null
    }

    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.value = AddProfileUiState.Loading
            try {
                withTimeout(15_000L) {
                    val profile = repository.getProfileById(profileId)
                    if (profile != null) {
                        currentProfileId = profile.profileId
                        currentAvatarUrl = profile.avatarUrl
                        _uiState.value = AddProfileUiState.Loaded(profile)
                    } else {
                        _uiState.value = AddProfileUiState.Error(R.string.error_profile_not_found)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddProfileUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun saveProfile(
        fullName: String,
        profileType: ProfileType,
        relationship: String,
        birthDate: Long,
        passingDate: Long?,
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            _uiState.value = AddProfileUiState.Loading
            try {
                withTimeout(15_000L) {
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserUid.isNullOrBlank()) {
                        throw IllegalStateException("Usuario no autenticado")
                    }

                    val profileId = currentProfileId ?: UUID.randomUUID().toString()
                    var avatarUrl = currentAvatarUrl ?: ""

                    if (imageBytes != null) {
                        val path = "images/profiles/${currentUserUid}_${profileId}.jpg"
                        val newUrl = storage.uploadImage(imageBytes, path)
                        
                        // Delete old image if it exists and is different from the new one
                        // (Though the path is the same, this handles cases where the path might change in the future,
                        // or ensures the object is cleanly overwritten/handled).
                        // Since Firebase Storage overwrites objects at the same path, deleting is optional if path matches, 
                        // but if we ever use unique UUIDs per upload, this is critical. 
                        // The Architect asked: "delete old image on upload of new". We will try to delete the old URL.
                        if (!currentAvatarUrl.isNullOrBlank() && currentAvatarUrl != newUrl) {
                             storage.deleteImage(currentAvatarUrl!!)
                        }
                        avatarUrl = newUrl
                    }

                    // Generar un ID local provisorio si no esperamos el server-side ID
                    val newProfile = Profile(
                        profileId = profileId, // FirebaseProfileRepository upserts this
                        creatorUserId = currentUserUid,
                        fullName = fullName.trim(),
                        profileType = profileType,
                        relationship = relationship.trim(),
                        birthDate = birthDate,
                        passingDate = passingDate,
                        avatarUrl = avatarUrl,
                        status = ProfileStatus.ACTIVE
                    )

                    repository.save(newProfile)
                    _uiState.value = AddProfileUiState.Success
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AddProfileUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                Log.e("FirestoreError", "Error al guardar altar", e)
                _uiState.value = AddProfileUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }
}
