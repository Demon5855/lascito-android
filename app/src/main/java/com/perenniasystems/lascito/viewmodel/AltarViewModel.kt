package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.MockMemoryRepository
import com.perenniasystems.lascito.data.repository.MockProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AltarUiState {
    data object Loading : AltarUiState()
    data class Success(val profile: Profile, val memories: List<Memory>) : AltarUiState()
    data class Error(val message: String) : AltarUiState()
}

class AltarViewModel(
    private val profileRepo: MockProfileRepository = MockProfileRepository(),
    private val memoryRepo:  MockMemoryRepository  = MockMemoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AltarUiState>(AltarUiState.Loading)
    val uiState: StateFlow<AltarUiState> = _uiState

    fun load(profileId: String) {
        viewModelScope.launch {
            _uiState.value = AltarUiState.Loading
            try {
                val profile  = profileRepo.getProfileById(profileId)
                    ?: throw Exception("Perfil no encontrado")
                val memories = memoryRepo.getMemoriesByProfile(profileId)
                _uiState.value = AltarUiState.Success(profile, memories)
            } catch (e: Exception) {
                _uiState.value = AltarUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
