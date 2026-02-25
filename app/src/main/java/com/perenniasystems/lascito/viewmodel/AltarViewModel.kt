package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.FirebaseMemoryRepository
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.MemoryRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import com.perenniasystems.lascito.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AltarUiState {
    data object Loading : AltarUiState()
    data class Success(val profile: Profile, val memories: List<Memory>) : AltarUiState()
    /** [messageResId] is an R.string resource ID — resolved in the UI layer. */
    data class Error(val messageResId: Int) : AltarUiState()
}

class AltarViewModel(
    private val profileRepo: ProfileRepository = FirebaseProfileRepository(),
    private val memoryRepo:  MemoryRepository  = FirebaseMemoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AltarUiState>(AltarUiState.Loading)
    val uiState: StateFlow<AltarUiState> = _uiState

    fun load(profileId: String) {
        viewModelScope.launch {
            _uiState.value = AltarUiState.Loading
            try {
                val profile  = profileRepo.getProfileById(profileId)
                    ?: throw Exception("not_found")
                val memories = memoryRepo.getMemoriesByProfile(profileId)
                _uiState.value = AltarUiState.Success(profile, memories)
            } catch (e: Exception) {
                _uiState.value = AltarUiState.Error(
                    if (e.message == "not_found") R.string.error_profile_not_found
                    else                          R.string.error_load_altars
                )
            }
        }
    }
}
