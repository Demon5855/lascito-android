package com.perenniasystems.lascito.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import com.perenniasystems.lascito.data.repository.MemoryRepository
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
import android.net.Uri
import kotlinx.coroutines.tasks.await

sealed interface AddMemoryUiState {
    data object Idle : AddMemoryUiState
    data object Loading : AddMemoryUiState
    data class Loaded(val memory: Memory) : AddMemoryUiState
    data object Success : AddMemoryUiState
    data class Error(@StringRes val messageResId: Int) : AddMemoryUiState
}

@HiltViewModel
class AddMemoryViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val storage: FirebaseStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddMemoryUiState>(AddMemoryUiState.Idle)
    val uiState: StateFlow<AddMemoryUiState> = _uiState.asStateFlow()

    private var currentMemoryId: String? = null
    private var currentMediaUrl: String? = null
    private var currentCapturedDate: Long? = null

    private val _selectedMediaUri = MutableStateFlow<Uri?>(null)
    val selectedMediaUri: StateFlow<Uri?> = _selectedMediaUri.asStateFlow()

    private val _selectedMediaType = MutableStateFlow<MediaType>(MediaType.IMAGE)
    val selectedMediaType: StateFlow<MediaType> = _selectedMediaType.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow("00:00")
    val recordingDuration: StateFlow<String> = _recordingDuration.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    fun updateSelectedMedia(uri: Uri?, type: MediaType) {
        _selectedMediaUri.value = uri
        _selectedMediaType.value = type
    }

    fun setRecordingState(isRecording: Boolean, duration: String = "00:00") {
        _isRecording.value = isRecording
        _recordingDuration.value = duration
    }

    fun clearMedia() {
        _selectedMediaUri.value = null
        _selectedMediaType.value = MediaType.IMAGE
        currentMediaUrl = null
        
        val currentState = _uiState.value
        if (currentState is AddMemoryUiState.Loaded) {
            // Copiamos el recuerdo actual pero con la URL vacía
            _uiState.value = AddMemoryUiState.Loaded(
                currentState.memory.copy(mediaUrl = "")
            )
        }
    }

    fun resetState() {
        _uiState.value = AddMemoryUiState.Idle
        currentMemoryId = null
        currentMediaUrl = null
        currentCapturedDate = null
    }

    fun loadMemory(memoryId: String, profileId: String) {
        viewModelScope.launch {
            _uiState.value = AddMemoryUiState.Loading
            try {
                withTimeout(15_000L) {
                    // Single-document fetch (O(1)): evita descargar toda la colección (O(N)).
                    val memory = repository.getMemoryById(profileId, memoryId)
                    if (memory != null) {
                        currentMemoryId = memory.memoryId
                        currentMediaUrl = memory.mediaUrl
                        currentCapturedDate = memory.capturedDate
                        _selectedMediaType.value = memory.mediaType // Vital para pintar Video, Audio o Imagen al editar
                        _uiState.value = AddMemoryUiState.Loaded(memory)
                    } else {
                        _uiState.value = AddMemoryUiState.Error(R.string.error_generic)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddMemoryUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun saveMemory(profileId: String, contextText: String, mediaType: MediaType = MediaType.IMAGE, imageBytes: ByteArray?, mediaUri: Uri? = null) {
        viewModelScope.launch {
            _uiState.value = AddMemoryUiState.Loading
            _uploadProgress.value = 0f
            try {
                // Extended timeout for large files (videos)
                withTimeout(120_000L) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        _uiState.value = AddMemoryUiState.Error(R.string.error_not_authenticated)
                        return@withTimeout
                    }
                    
                    try {
                        currentUser.reload().await()
                    } catch (e: Exception) {
                        _uiState.value = AddMemoryUiState.Error(R.string.error_session_expired)
                        return@withTimeout
                    }

                    if (currentUser.isEmailVerified == false) {
                        _uiState.value = AddMemoryUiState.Error(R.string.error_email_not_verified)
                        return@withTimeout
                    }

                    val memoryId = currentMemoryId ?: UUID.randomUUID().toString()
                    var mediaUrl = currentMediaUrl ?: ""

                    if (mediaType == MediaType.IMAGE && imageBytes != null) {
                        val path = "memories/${profileId}/images/${memoryId}.jpg"
                        val newUrl = storage.uploadImage(imageBytes, path)
                        
                        // Delete old image if it exists and is different from the new one
                        if (!currentMediaUrl.isNullOrBlank() && currentMediaUrl != newUrl) {
                            storage.deleteImage(currentMediaUrl!!)
                        }
                        mediaUrl = newUrl
                    } else if ((mediaType == MediaType.VIDEO || mediaType == MediaType.AUDIO) && mediaUri != null) {
                        val result = storage.uploadMediaFile(mediaUri, mediaType, profileId, onProgress = { progress ->
                            _uploadProgress.value = progress
                        })
                        if (result.isSuccess) {
                            val newUrl = result.getOrThrow()
                            if (!currentMediaUrl.isNullOrBlank() && currentMediaUrl != newUrl) {
                                storage.deleteImage(currentMediaUrl!!)
                            }
                            mediaUrl = newUrl
                            
                            // Cleanup local audio cache file
                            if (mediaType == MediaType.AUDIO && mediaUri.scheme == "file") {
                                mediaUri.path?.let { java.io.File(it).delete() }
                            }
                        } else {
                            throw result.exceptionOrNull() ?: Exception("Upload error")
                        }
                    }

                    val newMemory = Memory(
                        memoryId = memoryId,
                        profileId = profileId,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        storageClass = "STANDARD",
                        contextText = contextText.trim(),
                        capturedDate = currentCapturedDate ?: System.currentTimeMillis()
                    )

                    repository.save(newMemory)
                    _uploadProgress.value = 1f
                    delay(400) // Pequeña pausa psicológica
                    _uiState.value = AddMemoryUiState.Success
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AddMemoryUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = AddMemoryUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun resendVerificationEmail() {
        FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
    }
}
