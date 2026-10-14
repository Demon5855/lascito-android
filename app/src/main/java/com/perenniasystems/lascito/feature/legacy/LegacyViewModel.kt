package com.perenniasystems.lascito.feature.legacy

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.Legacy
import com.perenniasystems.lascito.data.repository.MemoryRepository
import com.perenniasystems.lascito.data.repository.LegacyRepository
import com.perenniasystems.lascito.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import com.perenniasystems.lascito.core.common.ErrorHandler

sealed class LegacyUiState {
    data object Loading : LegacyUiState()
    data class Success(val legacy: Legacy, val memories: List<Memory>) : LegacyUiState()
    /** [messageResId] is an R.string resource ID — resolved in the UI layer. */
    data class Error(val messageResId: Int) : LegacyUiState()
}

@HiltViewModel
class LegacyViewModel @Inject constructor(
    private val legacyRepo: LegacyRepository,
    private val memoryRepo:  MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LegacyUiState>(LegacyUiState.Loading)
    val uiState: StateFlow<LegacyUiState> = _uiState

    fun load(profileId: String) {
        viewModelScope.launch {
            _uiState.value = LegacyUiState.Loading
            try {
                withTimeout(15_000L) {
                    val legacy  = legacyRepo.getLegacyById(profileId)
                        ?: throw Exception("not_found")
                    val memories = memoryRepo.getMemoriesByLegacy(profileId)
                    _uiState.value = LegacyUiState.Success(legacy, memories)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // For simplicity, we can use a general error string or a new R.string.error_network if created
                // Here we fallback to R.string.error_load_legacies since the ui requires a ResId.
                // Or we can just log it and show error_load_legacies.
                _uiState.value = LegacyUiState.Error(R.string.error_load_legacies) // Or add res id for timeout
            } catch (e: IOException) {
                _uiState.value = LegacyUiState.Error(R.string.error_load_legacies) // Or add res id for network
            } catch (e: Exception) {
                _uiState.value = LegacyUiState.Error(
                    if (e.message?.equals("not_found") == true) R.string.error_legacy_not_found
                    else                                        R.string.error_load_legacies
                )
            }
        }
    }

    fun deleteMemory(memoryId: String, currentLegacyId: String, mediaUrl: String) {
        viewModelScope.launch {
            _uiState.value = LegacyUiState.Loading
            try {
                memoryRepo.deleteMemory(memoryId, currentLegacyId, mediaUrl)
                load(currentLegacyId) // Reload list after successful deletion
            } catch (e: Exception) {
                _uiState.value = LegacyUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }

    fun deleteLegacy(profileId: String, avatarUrl: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LegacyUiState.Loading
            try {
                legacyRepo.deleteLegacy(profileId, avatarUrl)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = LegacyUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }
}
