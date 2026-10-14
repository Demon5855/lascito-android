package com.perenniasystems.lascito.feature.legacy

import com.perenniasystems.lascito.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.InheritanceInvitation
import com.perenniasystems.lascito.data.model.Inheritance
import com.perenniasystems.lascito.data.model.Legacy
import com.perenniasystems.lascito.data.repository.LegacyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.annotation.StringRes
import com.perenniasystems.lascito.core.common.ErrorHandler
import java.io.IOException

/**
 * ViewModel for the Dashboard screen.
 *
 * Owns the UI state for the legacy list. The [LegacyRepository] is injected
 * to follow Dependency Inversion — swap implementation without touching this class.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: LegacyRepository,
    private val guardianRepository: com.perenniasystems.lascito.data.repository.GuardianRepository,
    private val session: SessionRepository
) : ViewModel() {

    companion object {
        // Feature flag para desarrollo (VISIBLE Y EXPLÍCITO)
        private const val FEATURE_CLAIM_INHERITANCE_ENABLED = true
    }

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // ── Email Verification State ──────────────────────────────────────────────
    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _isSendingVerification = MutableStateFlow(false)
    val isSendingVerification: StateFlow<Boolean> = _isSendingVerification.asStateFlow()

    private val _verificationEmailSent = MutableStateFlow(false)
    val verificationEmailSent: StateFlow<Boolean> = _verificationEmailSent.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                withTimeout(15_000L) {
                    val user = session.currentUser
                        ?: throw Exception("Not authenticated")

                    // Reload user to get latest email verification status
                    viewModelScope.launch {
                        _isEmailVerified.value = session.reloadUser()
                            .map { it.isEmailVerified }
                            .getOrDefault(user.isEmailVerified)
                    }

                    val legacies = repository.getLegacies(userId = user.uid)
                    _isEmailVerified.value = user.isEmailVerified

                    val pendingInvitations = loadPendingInvitations()
                    val claimedInheritances = guardianRepository.getClaimedInheritances()
                        .getOrDefault(emptyList())

                    _uiState.value = DashboardUiState.Success(legacies, pendingInvitations, claimedInheritances)
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

    private suspend fun loadPendingInvitations(): List<InheritanceInvitation> {
        if (!FEATURE_CLAIM_INHERITANCE_ENABLED) return emptyList()
        return guardianRepository.getPendingInvitations().getOrDefault(emptyList())
    }

    fun deleteLegacy(profileId: String, imageUrl: String?) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                repository.deleteLegacy(profileId, imageUrl)
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

    fun resetVerificationBannerState() {
        _verificationEmailSent.value = false
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _isSendingVerification.value = true
            try {
                withTimeout(15_000L) {
                    session.resendVerificationEmail().getOrThrow()
                }
                _verificationEmailSent.value = true

            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = DashboardUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    messageResId = ErrorHandler.mapExceptionToResId(e)
                )
            } finally {
                _isSendingVerification.value = false
            }
        }
    }
}

// ── Sealed UI State ────────────────────────────────────────────────────────────
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val legacies: List<Legacy>,
        val pendingInvitations: List<InheritanceInvitation> = emptyList(),
        val claimedInheritances: List<Inheritance> = emptyList()
    ) : DashboardUiState
    data class Error(@StringRes val messageResId: Int) : DashboardUiState
}
