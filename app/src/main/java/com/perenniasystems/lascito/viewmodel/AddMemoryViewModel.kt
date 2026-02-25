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

sealed interface AddMemoryUiState {
    data object Idle : AddMemoryUiState
    data object Loading : AddMemoryUiState
    data object Success : AddMemoryUiState
    data class Error(@StringRes val messageResId: Int) : AddMemoryUiState
}

class AddMemoryViewModel(
    private val repository: MemoryRepository = FirebaseMemoryRepository(),
    private val storage: FirebaseStorageRepository = FirebaseStorageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddMemoryUiState>(AddMemoryUiState.Idle)
    val uiState: StateFlow<AddMemoryUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AddMemoryUiState.Idle
    }

    fun saveMemory(profileId: String, contextText: String, mediaType: MediaType = MediaType.IMAGE, imageBytes: ByteArray?) {
        viewModelScope.launch {
            _uiState.value = AddMemoryUiState.Loading
            try {
                withTimeout(15_000L) {
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserUid.isNullOrBlank()) {
                        throw IllegalStateException("Usuario no autenticado")
                    }

                    val memoryId = UUID.randomUUID().toString()
                    var mediaUrl = ""

                    if (imageBytes != null) {
                        val path = "images/memories/${profileId}/${memoryId}.jpg"
                        mediaUrl = storage.uploadImage(imageBytes, path)
                    }

                    val newMemory = Memory(
                        memoryId = memoryId,
                        profileId = profileId,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        storageClass = "STANDARD",
                        contextText = contextText.trim(),
                        capturedDate = System.currentTimeMillis()
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
