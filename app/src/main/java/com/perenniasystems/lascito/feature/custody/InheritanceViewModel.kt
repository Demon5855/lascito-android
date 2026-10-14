package com.perenniasystems.lascito.feature.custody

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface InheritanceUiState {
    data object Loading : InheritanceUiState
    data class Success(val creatorName: String, val legacies: List<Profile>) : InheritanceUiState
    data class Error(@StringRes val messageResId: Int) : InheritanceUiState
}

/**
 * Herencia ya reclamada: los legados del creador, en solo lectura para el guardián.
 * El acceso lo conceden las reglas de Firestore mientras la invitación esté
 * CLAIMED con el UID del guardián.
 */
@HiltViewModel
class InheritanceViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val guardianRepo: GuardianRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InheritanceUiState>(InheritanceUiState.Loading)
    val uiState: StateFlow<InheritanceUiState> = _uiState.asStateFlow()

    fun load(creatorUid: String) {
        viewModelScope.launch {
            _uiState.value = InheritanceUiState.Loading
            try {
                withTimeout(15_000L) {
                    val inheritance = guardianRepo.getClaimedInheritances().getOrThrow()
                        .firstOrNull { it.creatorUid == creatorUid }
                        ?: throw NoSuchElementException("Inheritance not claimed")
                    val legacies = profileRepo.getProfiles(userId = creatorUid)
                    _uiState.value = InheritanceUiState.Success(inheritance.creatorName, legacies)
                }
            } catch (e: Exception) {
                _uiState.value = InheritanceUiState.Error(R.string.error_load_inheritance)
            }
        }
    }
}
