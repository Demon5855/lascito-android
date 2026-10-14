package com.perenniasystems.lascito.feature.legacy

import com.perenniasystems.lascito.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Legacy
import com.perenniasystems.lascito.data.model.LegacyStatus
import com.perenniasystems.lascito.data.model.LegacyType
import com.perenniasystems.lascito.data.repository.FirebaseLegacyRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import com.perenniasystems.lascito.data.repository.LegacyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.core.common.ErrorHandler
import java.io.IOException
import java.util.UUID

sealed interface AddLegacyUiState {
    data object Idle : AddLegacyUiState
    data object Loading : AddLegacyUiState
    data class Loaded(val legacy: Legacy) : AddLegacyUiState
    data object Success : AddLegacyUiState
    data class Error(@StringRes val messageResId: Int) : AddLegacyUiState
}

@HiltViewModel
class AddLegacyViewModel @Inject constructor(
    private val repository: LegacyRepository,
    private val storage: FirebaseStorageRepository,
    private val session: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddLegacyUiState>(AddLegacyUiState.Idle)
    val uiState: StateFlow<AddLegacyUiState> = _uiState.asStateFlow()

    private var currentLegacyId: String? = null
    private var currentAvatarUrl: String? = null

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    fun resetState() {
        _uiState.value = AddLegacyUiState.Idle
        currentLegacyId = null
        currentAvatarUrl = null
    }

    fun loadLegacy(profileId: String) {
        viewModelScope.launch {
            _uiState.value = AddLegacyUiState.Loading
            try {
                withTimeout(15_000L) {
                    val legacy = repository.getLegacyById(profileId)
                    if (legacy != null) {
                        currentLegacyId = legacy.profileId
                        currentAvatarUrl = legacy.avatarUrl
                        _uiState.value = AddLegacyUiState.Loaded(legacy)
                    } else {
                        _uiState.value = AddLegacyUiState.Error(R.string.error_legacy_not_found)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddLegacyUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun saveLegacy(
        fullName: String,
        profileType: LegacyType,
        relationship: String,
        birthDate: Long,
        passingDate: Long?,
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            _uiState.value = AddLegacyUiState.Loading
            _uploadProgress.value = 0f
            try {
                withTimeout(15_000L) {
                    if (session.currentUser == null) {
                        _uiState.value = AddLegacyUiState.Error(R.string.error_not_authenticated)
                        return@withTimeout
                    }

                    val currentUser = session.reloadUser().getOrElse { _ ->
                        _uiState.value = AddLegacyUiState.Error(R.string.error_session_expired)
                        return@withTimeout
                    }

                    if (!currentUser.isEmailVerified) {
                        _uiState.value = AddLegacyUiState.Error(R.string.error_email_not_verified)
                        return@withTimeout
                    }

                    _uploadProgress.value = 0.3f
                    val profileId = currentLegacyId ?: UUID.randomUUID().toString()
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
                    val newLegacy = Legacy(
                        profileId = profileId, // FirebaseLegacyRepository upserts this
                        creatorUserId = currentUser.uid,
                        fullName = fullName.trim(),
                        profileType = profileType,
                        relationship = relationship.trim(),
                        birthDate = birthDate,
                        passingDate = passingDate,
                        avatarUrl = finalAvatarUrl,
                        status = LegacyStatus.ACTIVE
                    )

                    repository.save(newLegacy)
                    _uploadProgress.value = 1f
                    delay(400)
                    _uiState.value = AddLegacyUiState.Success
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AddLegacyUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = AddLegacyUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch { session.resendVerificationEmail() }
    }
}
