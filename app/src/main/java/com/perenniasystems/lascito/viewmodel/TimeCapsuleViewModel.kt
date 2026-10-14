package com.perenniasystems.lascito.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.model.TimeCapsule
import com.perenniasystems.lascito.data.model.TriggerType
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
    val capsuleId:         String       = UUID.randomUUID().toString(),
    val title:             String       = "",
    val content:           String       = "",
    val mediaUri:          Uri?         = null,
    val mediaUrl:          String?      = null,
    val mediaType:         MediaType    = MediaType.IMAGE,
    val triggerType:       TriggerType  = TriggerType.IMMEDIATE_POST_MORTEM,
    val scheduledDate:     Long?        = null,
    val isLoading:         Boolean      = false,
    val uploadProgress:    Float?       = null,
    val isSuccess:         Boolean      = false,
    
    // Estados de validación
    @androidx.annotation.StringRes val titleError: Int? = null,
    @androidx.annotation.StringRes val contentMediaError: Int? = null,
    @androidx.annotation.StringRes val dateError: Int? = null,
    @androidx.annotation.StringRes val errorMessageResId: Int? = null,
    
    // Estado transitorio
    val isSaveEnabled:     Boolean      = false
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class TimeCapsuleViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val storageRepository:  FirebaseStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapsuleUiState())
    val uiState: StateFlow<CapsuleUiState> = _uiState.asStateFlow()

    fun loadCapsule(capsuleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            guardianRepository.getTimeCapsule(capsuleId).fold(
                onSuccess = { capsule ->
                    _uiState.update {
                        it.copy(
                            capsuleId = capsule.capsuleId,
                            title = capsule.title,
                            content = capsule.content ?: "",
                            mediaUrl = capsule.mediaUrl,
                            mediaType = capsule.mediaType,
                            triggerType = capsule.triggerType,
                            scheduledDate = capsule.scheduledUnlockDate,
                            isLoading = false
                        )
                    }
                    validateForm()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            titleError = com.perenniasystems.lascito.R.string.error_generic
                        )
                    }
                }
            )
        }
    }

    // ── Mutadores de estado local ─────────────────────────────────────────────

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
        validateForm()
    }
    
    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
        validateForm()
    }
    
    fun onTriggerTypeChange(t: TriggerType) {
        _uiState.update {
            it.copy(
                triggerType   = t,
                scheduledDate = if (t == TriggerType.IMMEDIATE_POST_MORTEM) null else it.scheduledDate
            )
        }
        validateForm()
    }
    
    fun onScheduledDateChange(ms: Long?) {
        _uiState.update { it.copy(scheduledDate = ms) }
        validateForm()
    }
    
    fun onMediaSelected(uri: Uri, type: MediaType) {
        _uiState.update { it.copy(mediaUri = uri, mediaType = type) }
        validateForm()
    }
    
    fun clearMedia() {
        _uiState.update { it.copy(mediaUri = null, mediaType = MediaType.IMAGE) }
        validateForm()
    }
    
    fun clearError() = _uiState.update { it.copy(errorMessageResId = null) }
    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false) }

    // ── Validación Centralizada ───────────────────────────────────────────────
    
    private fun validateForm() {
        val state = _uiState.value
        
        // Regla 1: Título obligatorio
        val titleValid = state.title.isNotBlank()
        val titleError = if (!titleValid) com.perenniasystems.lascito.R.string.error_title_required else null
        
        // Regla 2: Contenido O Media obligatorio
        val hasContent = state.content.isNotBlank() || 
                        state.mediaUri != null || 
                        !state.mediaUrl.isNullOrBlank()
        val contentMediaError = if (!hasContent) 
            com.perenniasystems.lascito.R.string.error_content_or_media_required 
        else null
        
        // Regla 3: Si trigger es POST_MORTEM_DATE, fecha obligatoria
        val dateValid = state.triggerType != TriggerType.POST_MORTEM_DATE ||
                       state.scheduledDate != null
        val dateError = if (!dateValid) 
            com.perenniasystems.lascito.R.string.error_date_required 
        else null
        
        val isValid = titleValid && hasContent && dateValid
        
        _uiState.update {
            it.copy(
                titleError = titleError,
                contentMediaError = contentMediaError,
                dateError = dateError,
                isSaveEnabled = isValid && !it.isLoading
            )
        }
    }

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
        if (!state.isSaveEnabled) return

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
                    withTimeout(120_000L) {
                        val contentResolver = com.google.firebase.FirebaseApp.getInstance().applicationContext.contentResolver
                        val mimeType = contentResolver.getType(state.mediaUri)
                        var extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        
                        // Fallback seguro si MimeTypeMap falla
                        if (extension == null) {
                            extension = if (state.mediaType == MediaType.VIDEO) "mp4" else "jpg"
                        }

                        storageRepository.uploadCapsuleMedia(
                            uri       = state.mediaUri,
                            userId    = currentUser.uid,
                            capsuleId = state.capsuleId,
                            extension = extension,
                            onProgress = { progress ->
                                _uiState.update { it.copy(uploadProgress = progress) }
                            }
                        ).getOrThrow()
                    }
                } else {
                    state.mediaUrl // Retenemos URL previa (Modo edición sin cambiar archivo)
                }

                // 2. Construcción del documento
                val capsule = TimeCapsule(
                    capsuleId           = state.capsuleId,
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
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        uploadProgress = null,
                        errorMessageResId = com.perenniasystems.lascito.R.string.error_network
                    )
                }
            } catch (e: Exception) {
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
