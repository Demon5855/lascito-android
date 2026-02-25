package com.perenniasystems.lascito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.MediaType
import com.perenniasystems.lascito.data.repository.FirebaseMemoryRepository
import com.perenniasystems.lascito.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AddMemoryUiState {
    data object Idle : AddMemoryUiState
    data object Loading : AddMemoryUiState
    data object Success : AddMemoryUiState
    data class Error(val message: String) : AddMemoryUiState
}

class AddMemoryViewModel(
    private val repository: MemoryRepository = FirebaseMemoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddMemoryUiState>(AddMemoryUiState.Idle)
    val uiState: StateFlow<AddMemoryUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AddMemoryUiState.Idle
    }

    fun saveMemory(profileId: String, contextText: String, mediaType: MediaType = MediaType.IMAGE) {
        viewModelScope.launch {
            _uiState.value = AddMemoryUiState.Loading
            try {
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserUid.isNullOrBlank()) {
                    throw IllegalStateException("Usuario no autenticado")
                }

                val newMemory = Memory(
                    memoryId = UUID.randomUUID().toString(),
                    profileId = profileId,
                    mediaType = mediaType,
                    mediaUrl = "", // Empty for Phase 2, Storage logic comes later
                    storageClass = "STANDARD",
                    contextText = contextText.trim(),
                    capturedDate = System.currentTimeMillis()
                )

                repository.save(newMemory)
                _uiState.value = AddMemoryUiState.Success
            } catch (e: Exception) {
                Log.e("FirestoreError", "Error al guardar memoria", e)
                _uiState.value = AddMemoryUiState.Error(e.localizedMessage ?: "Error al subir memoria")
            }
        }
    }
}
