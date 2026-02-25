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
import kotlinx.coroutines.withTimeout
import androidx.annotation.StringRes
import com.perenniasystems.lascito.util.ErrorHandler
import java.io.IOException

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
            try {
                withTimeout(15_000L) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                        ?: throw Exception("Not authenticated")
                    val profiles = repository.getProfiles(userId = uid)
                    _uiState.value = DashboardUiState.Success(profiles)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = DashboardUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }

    fun deleteProfile(profileId: String, imageUrl: String?) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                repository.deleteProfile(profileId, imageUrl)
                refresh() // Reload list after successful deletion
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }

    fun resetState() {
        refresh()
    }
}

// ── Sealed UI State ────────────────────────────────────────────────────────────
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val profiles: List<Profile>) : DashboardUiState
    data class Error(@StringRes val messageResId: Int) : DashboardUiState
}
