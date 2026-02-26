package com.perenniasystems.lascito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.repository.FirebaseMemoryRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepositoryImpl
import com.perenniasystems.lascito.data.repository.MemoryRepository
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
import android.net.Uri

sealed interface AddMemoryUiState {
    data object Idle : AddMemoryUiState
    data object Loading : AddMemoryUiState
    data class Loaded(val memory: Memory) : AddMemoryUiState
    data object Success : AddMemoryUiState
    data class Error(@StringRes val messageResId: Int) : AddMemoryUiState
}

class AddMemoryViewModel(
    private val repository: MemoryRepository = FirebaseMemoryRepository(),
    private val storage: FirebaseStorageRepository = FirebaseStorageRepositoryImpl()
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

    fun updateSelectedMedia(uri: Uri?, type: MediaType) {
        _selectedMediaUri.value = uri
        _selectedMediaType.value = type
    }

    fun setRecordingState(isRecording: Boolean, duration: String = "00:00") {
        _isRecording.value = isRecording
        _recordingDuration.value = duration
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
                    // We need a getMemoryById from the repository, so I'll assume we can fetch it
                    // Actually, let's look at MemoryRepository: it only has getMemoriesByProfile. 
                    // To avoid another backend call if we don't have getMemoryById, we can just fetch all by profile and find it:
                    val memories = repository.getMemoriesByProfile(profileId)
                    val memory = memories.find { it.memoryId == memoryId }
                    if (memory != null) {
                        currentMemoryId = memory.memoryId
                        currentMediaUrl = memory.mediaUrl
                        currentCapturedDate = memory.capturedDate
                        _uiState.value = AddMemoryUiState.Loaded(memory)
                    } else {
                        // Resusing a generic profile not found or just generic error
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
            try {
                // Extended timeout for large files (videos)
                withTimeout(120_000L) {
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserUid.isNullOrBlank()) {
                        throw IllegalStateException("Usuario no autenticado")
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
                        val result = storage.uploadMediaFile(mediaUri, mediaType, profileId)
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
                    _uiState.value = AddMemoryUiState.Success
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AddMemoryUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                Log.e("FirestoreError", "Error al guardar memoria", e)
                _uiState.value = AddMemoryUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }
}
