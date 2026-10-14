package com.perenniasystems.lascito.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.MemoryRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import com.perenniasystems.lascito.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import com.perenniasystems.lascito.util.ErrorHandler

sealed class AltarUiState {
    data object Loading : AltarUiState()
    data class Success(val profile: Profile, val memories: List<Memory>) : AltarUiState()
    /** [messageResId] is an R.string resource ID — resolved in the UI layer. */
    data class Error(val messageResId: Int) : AltarUiState()
}

@HiltViewModel
class AltarViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val memoryRepo:  MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AltarUiState>(AltarUiState.Loading)
    val uiState: StateFlow<AltarUiState> = _uiState

    fun load(profileId: String) {
        viewModelScope.launch {
            _uiState.value = AltarUiState.Loading
            try {
                withTimeout(15_000L) {
                    val profile  = profileRepo.getProfileById(profileId)
                        ?: throw Exception("not_found")
                    val memories = memoryRepo.getMemoriesByProfile(profileId)
                    _uiState.value = AltarUiState.Success(profile, memories)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // For simplicity, we can use a general error string or a new R.string.error_network if created
                // Here we fallback to R.string.error_load_altars since the ui requires a ResId.
                // Or we can just log it and show error_load_altars.
                _uiState.value = AltarUiState.Error(R.string.error_load_altars) // Or add res id for timeout
            } catch (e: IOException) {
                _uiState.value = AltarUiState.Error(R.string.error_load_altars) // Or add res id for network
            } catch (e: Exception) {
                _uiState.value = AltarUiState.Error(
                    if (e.message?.equals("not_found") == true) R.string.error_profile_not_found
                    else                                        R.string.error_load_altars
                )
            }
        }
    }

    fun deleteMemory(memoryId: String, currentProfileId: String, mediaUrl: String) {
        viewModelScope.launch {
            _uiState.value = AltarUiState.Loading
            try {
                memoryRepo.deleteMemory(memoryId, currentProfileId, mediaUrl)
                load(currentProfileId) // Reload list after successful deletion
            } catch (e: Exception) {
                _uiState.value = AltarUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }

    fun deleteProfile(profileId: String, avatarUrl: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AltarUiState.Loading
            try {
                profileRepo.deleteProfile(profileId, avatarUrl)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AltarUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }
}
