package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.MemoryRepository
import com.perenniasystems.lascito.data.repository.MockMemoryRepository
import com.perenniasystems.lascito.data.repository.MockProfileRepository
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
    // Depend on interfaces — swap impl in Phase 2 without touching this class.
    private val profileRepo: ProfileRepository = MockProfileRepository(),
    private val memoryRepo:  MemoryRepository  = MockMemoryRepository()
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
