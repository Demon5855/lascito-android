package com.perenniasystems.lascito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AddProfileUiState {
    data object Idle : AddProfileUiState
    data object Loading : AddProfileUiState
    data object Success : AddProfileUiState
    data class Error(val message: String) : AddProfileUiState
}

class AddProfileViewModel(
    private val repository: ProfileRepository = FirebaseProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddProfileUiState>(AddProfileUiState.Idle)
    val uiState: StateFlow<AddProfileUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AddProfileUiState.Idle
    }

    fun saveProfile(
        fullName: String,
        profileType: ProfileType,
        relationship: String,
        birthDate: Long,
        passingDate: Long?
    ) {
        viewModelScope.launch {
            _uiState.value = AddProfileUiState.Loading
            try {
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserUid.isNullOrBlank()) {
                    throw IllegalStateException("Usuario no autenticado")
                }

                // Generar un ID local provisorio si no esperamos el server-side ID
                val newProfile = Profile(
                    profileId = UUID.randomUUID().toString(), // FirebaseProfileRepository puede o no sobrescribir esto
                    creatorUserId = currentUserUid,
                    fullName = fullName.trim(),
                    profileType = profileType,
                    relationship = relationship.trim(),
                    birthDate = birthDate,
                    passingDate = passingDate,
                    avatarUrl = "", // Empty for Phase 2, Storage logic comes later
                    status = ProfileStatus.ACTIVE
                )

                repository.save(newProfile)
                _uiState.value = AddProfileUiState.Success
            } catch (e: Exception) {
                Log.e("FirestoreError", "Error al guardar altar", e)
                _uiState.value = AddProfileUiState.Error(e.localizedMessage ?: "Error desconocido al guardar")
            }
        }
    }
}
