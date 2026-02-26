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
import kotlinx.coroutines.delay
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

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

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
            _uploadProgress.value = 0f
            try {
                withTimeout(15_000L) {
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserUid.isNullOrBlank()) {
                        throw IllegalStateException("Usuario no autenticado")
                    }

                    _uploadProgress.value = 0.3f
                    val profileId = currentProfileId ?: UUID.randomUUID().toString()
                    val finalAvatarUrl = if (imageBytes != null) {
                        val path = "profiles/covers/${profileId}.jpg"
                        val url = storage.uploadImage(imageBytes, path)
                        _uploadProgress.value = 0.6f
                        url
                    } else {
                        currentAvatarUrl ?: ""
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
                        avatarUrl = finalAvatarUrl,
                        status = ProfileStatus.ACTIVE
                    )

                    repository.save(newProfile)
                    _uploadProgress.value = 1f
                    delay(400)
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
