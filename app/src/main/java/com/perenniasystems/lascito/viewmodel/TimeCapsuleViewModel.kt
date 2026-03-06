package com.perenniasystems.lascito.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.model.TimeCapsule
import com.perenniasystems.lascito.data.model.TriggerType
import com.perenniasystems.lascito.data.repository.FirebaseGuardianRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepositoryImpl
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class CapsuleUiState(
    val title:             String       = "",
    val content:           String       = "",
    /** URI local del archivo multimedia elegido por el usuario (aún no subido). */
    val mediaUri:          Uri?         = null,
    val mediaType:         MediaType    = MediaType.IMAGE,
    val triggerType:       TriggerType  = TriggerType.IMMEDIATE_POST_MORTEM,
    /**
     * Timestamp (ms) de la fecha de apertura programada.
     * Solo relevante cuando [triggerType] == [TriggerType.POST_MORTEM_DATE].
     */
    val scheduledDate:     Long?        = null,
    val isLoading:         Boolean      = false,
    /** Progreso de subida al Storage (0f…1f). null = sin archivo o sin progreso conocido. */
    val uploadProgress:    Float?       = null,
    val isSuccess:         Boolean      = false,
    @androidx.annotation.StringRes val errorMessageResId: Int? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class TimeCapsuleViewModel(
    private val guardianRepository: GuardianRepository         = FirebaseGuardianRepository(),
    private val storageRepository:  FirebaseStorageRepository  = FirebaseStorageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapsuleUiState())
    val uiState: StateFlow<CapsuleUiState> = _uiState.asStateFlow()

    // ── Mutadores de estado local ─────────────────────────────────────────────

    fun onTitleChange(title: String)      = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String)  = _uiState.update { it.copy(content = content) }
    fun onTriggerTypeChange(t: TriggerType) = _uiState.update {
        it.copy(
            triggerType   = t,
            // Resetea la fecha si vuelven a IMMEDIATE_POST_MORTEM
            scheduledDate = if (t == TriggerType.IMMEDIATE_POST_MORTEM) null else it.scheduledDate
        )
    }
    fun onScheduledDateChange(ms: Long?)  = _uiState.update { it.copy(scheduledDate = ms) }
    fun onMediaSelected(uri: Uri, type: MediaType) =
        _uiState.update { it.copy(mediaUri = uri, mediaType = type) }
    fun clearMedia()      = _uiState.update { it.copy(mediaUri = null, mediaType = MediaType.IMAGE) }
    fun clearError()      = _uiState.update { it.copy(errorMessageResId = null) }
    fun resetSuccess()    = _uiState.update { it.copy(isSuccess = false) }

    // ── Persistencia ──────────────────────────────────────────────────────────

    /**
     * Guarda la Cápsula del Tiempo:
     *  1. Si hay [CapsuleUiState.mediaUri], sube el archivo a Firebase Storage
     *     reutilizando [FirebaseStorageRepository.uploadMediaFile] con cancelación
     *     automática ante timeout del ViewModel.
     *  2. Construye el objeto [TimeCapsule] con UUID generado en cliente.
     *  3. Persiste el documento vía [GuardianRepository.createTimeCapsule].
     */
    fun saveCapsule() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessageResId = null, uploadProgress = null) }
            
            // Verificación del usuario y del email
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessageResId = com.perenniasystems.lascito.R.string.error_not_authenticated) }
                return@launch
            }
            
            try {
                currentUser.reload().await()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessageResId = com.perenniasystems.lascito.R.string.error_session_expired) }
                return@launch
            }
            
            if (currentUser.isEmailVerified == false) {
                _uiState.update { it.copy(isLoading = false, errorMessageResId = com.perenniasystems.lascito.R.string.error_email_not_verified) }
                return@launch
            }

            try {
                // 1. Subida de multimedia (opcional)
                val remoteMediaUrl: String? = if (state.mediaUri != null) {
                    withTimeout(120_000L) {   // 2 min máx para archivos grandes
                        storageRepository.uploadMediaFile(
                            uri       = state.mediaUri,
                            mediaType = state.mediaType,
                            profileId = "capsules",           // carpeta dedicada en Storage
                            onProgress = { progress ->
                                _uiState.update { it.copy(uploadProgress = progress) }
                            }
                        ).getOrThrow()
                    }
                } else null

                // 2. Construcción del documento
                val capsule = TimeCapsule(
                    capsuleId           = UUID.randomUUID().toString(),
                    title               = state.title.trim(),
                    content             = state.content.trim().ifBlank { null },
                    mediaUrl            = remoteMediaUrl,
                    mediaType           = if (remoteMediaUrl != null) state.mediaType else MediaType.IMAGE,
                    triggerType         = state.triggerType,
                    scheduledUnlockDate = if (state.triggerType == TriggerType.POST_MORTEM_DATE)
                                             state.scheduledDate else null,
                    isLocked            = true,
                    createdAt           = System.currentTimeMillis()
                )

                // 3. Persistencia en Firestore
                guardianRepository.createTimeCapsule(capsule).getOrThrow()

                _uiState.update { it.copy(isLoading = false, uploadProgress = null, isSuccess = true) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("TimeCapsuleViewModel", "Timeout saving capsule", e)
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        uploadProgress = null,
                        errorMessageResId = com.perenniasystems.lascito.R.string.error_network
                    )
                }
            } catch (e: Exception) {
                Log.e("TimeCapsuleViewModel", "Error saving capsule", e)
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        uploadProgress = null,
                        errorMessageResId = com.perenniasystems.lascito.util.ErrorHandler.mapExceptionToResId(e)
                    )
                }
            }
        }
    }
}
