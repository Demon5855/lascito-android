package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.MockProfileRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 *
 * Owns the UI state for the profile list. The [ProfileRepository] is injected
 * to follow Dependency Inversion — in Phase 2 swap [MockProfileRepository]
 * for a real Firestore/DynamoDB implementation without touching this class.
 */
class DashboardViewModel(
    private val repository: ProfileRepository = MockProfileRepository()
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            runCatching {
                // Mock user ID — will come from Firebase Auth in Phase 2
                repository.getProfiles(userId = "user-mock-001")
            }.onSuccess { profiles ->
                _uiState.value = DashboardUiState.Success(profiles)
            }.onFailure { error ->
                _uiState.value = DashboardUiState.Error(
                    message = error.localizedMessage ?: "Error desconocido"
                )
            }
        }
    }
}

// ── Sealed UI State ────────────────────────────────────────────────────────────
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val profiles: List<Profile>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
