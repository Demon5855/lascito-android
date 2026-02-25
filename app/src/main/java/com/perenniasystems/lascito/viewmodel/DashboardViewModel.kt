package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 *
 * Owns the UI state for the profile list. The [ProfileRepository] is injected
 * to follow Dependency Inversion — swap implementation without touching this class.
 */
class DashboardViewModel(
    private val repository: ProfileRepository = FirebaseProfileRepository()
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            runCatching {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("Not authenticated")
                repository.getProfiles(userId = uid)
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
