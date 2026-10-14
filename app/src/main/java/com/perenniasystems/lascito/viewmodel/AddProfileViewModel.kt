package com.perenniasystems.lascito.viewmodel

import com.perenniasystems.lascito.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
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

@HiltViewModel
class AddProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val storage: FirebaseStorageRepository,
    private val session: SessionRepository
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
                    if (session.currentUser == null) {
                        _uiState.value = AddProfileUiState.Error(R.string.error_not_authenticated)
                        return@withTimeout
                    }

                    val currentUser = session.reloadUser().getOrElse { _ ->
                        _uiState.value = AddProfileUiState.Error(R.string.error_session_expired)
                        return@withTimeout
                    }

                    if (!currentUser.isEmailVerified) {
                        _uiState.value = AddProfileUiState.Error(R.string.error_email_not_verified)
                        return@withTimeout
                    }

                    _uploadProgress.value = 0.3f
                    val profileId = currentProfileId ?: UUID.randomUUID().toString()
                    val finalAvatarUrl = if (imageBytes != null) {
                        val path = "profiles/covers/${currentUser.uid}/${profileId}.jpg"
                        val url = storage.uploadImage(imageBytes, path)
                        // Portada anterior en otra ruta (esquema previo sin UID): se limpia.
                        val previousUrl = currentAvatarUrl
                        if (!previousUrl.isNullOrBlank() && !previousUrl.contains(Uri.encode(path))) {
                            storage.deleteImage(previousUrl)
                        }
                        _uploadProgress.value = 0.6f
                        url
                    } else {
                        currentAvatarUrl ?: ""
                    }

                    // Generar un ID local provisorio si no esperamos el server-side ID
                    val newProfile = Profile(
                        profileId = profileId, // FirebaseProfileRepository upserts this
                        creatorUserId = currentUser.uid,
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
                _uiState.value = AddProfileUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch { session.resendVerificationEmail() }
    }
}
