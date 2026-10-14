package com.perenniasystems.lascito.feature.custody

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.InvalidPinException
import com.perenniasystems.lascito.data.model.InheritanceBlockedException
import com.perenniasystems.lascito.data.model.InheritanceExpiredException
import com.perenniasystems.lascito.data.model.InheritanceNotConfiguredException
import com.perenniasystems.lascito.data.model.Inheritance
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.core.common.ValidationRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClaimInheritanceUiState(
    val pin: String = "",
    val pinError: Int? = null,
    val isLoading: Boolean = false,
    val isUnlockEnabled: Boolean = false,
    val unlockError: Int? = null,
    val unlockSuccess: Boolean = false,
    val attemptsRemaining: Int = 3,
    val isLocked: Boolean = false,
    val inheritance: Inheritance? = null,
    /** Creador de la invitación: indica al guardián de quién es el PIN que debe escribir. */
    val creatorName: String = "",
    val isInvitationLoaded: Boolean = false
)

@HiltViewModel
class ClaimInheritanceViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClaimInheritanceUiState())
    val uiState: StateFlow<ClaimInheritanceUiState> = _uiState.asStateFlow()

    private var invitationId: String? = null

    /** Carga la invitación que se va a reclamar para mostrar de quién es. */
    fun load(invitationId: String) {
        if (this.invitationId == invitationId) return
        this.invitationId = invitationId

        viewModelScope.launch {
            guardianRepository.getPendingInvitation(invitationId)
                .onSuccess { invitation ->
                    if (invitation == null) {
                        _uiState.update { it.copy(unlockError = R.string.error_inheritance_not_available) }
                    } else {
                        _uiState.update {
                            it.copy(creatorName = invitation.creatorName, isInvitationLoaded = true)
                        }
                        updateUnlockButtonState()
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(unlockError = R.string.error_load_inheritance) }
                }
        }
    }

    fun onPinChanged(pin: String) {
        val digitsOnly = pin.filter { it.isDigit() }
        if (digitsOnly.length > 4) return

        _uiState.update {
            it.copy(
                pin = digitsOnly,
                pinError = ValidationRules.validatePin(digitsOnly),
                unlockError = null // Limpiar error anterior al escribir
            )
        }
        updateUnlockButtonState()
    }

    private fun updateUnlockButtonState() {
        val state = _uiState.value
        val isValid = state.pinError == null && state.pin.isNotEmpty()
        _uiState.update {
            it.copy(isUnlockEnabled = isValid && state.isInvitationLoaded && !state.isLoading)
        }
    }

    fun claimVault() {
        val state = _uiState.value
        val invitationId = invitationId ?: return
        if (!state.isUnlockEnabled || state.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    unlockError = null
                )
            }

            try {
                val inheritance = guardianRepository.claimInheritance(
                    invitationId = invitationId,
                    pin = state.pin
                ).getOrThrow()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        unlockSuccess = true,
                        inheritance = inheritance
                    )
                }

            } catch (e: InvalidPinException) {
                // FASE 4.2 Error path - Excepción del Backend
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        unlockError = R.string.error_incorrect_pin,
                        attemptsRemaining = e.attemptsRemaining
                    )
                }
            } catch (e: InheritanceBlockedException) {
                // Backend dictamina que se acabaron los intentos
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLocked = true,
                        attemptsRemaining = 0,
                        unlockError = R.string.error_too_many_attempts
                    )
                }
            } catch (e: InheritanceNotConfiguredException) {
                _uiState.update {
                    it.copy(isLoading = false, unlockError = R.string.error_inheritance_not_configured)
                }
            } catch (e: InheritanceExpiredException) {
                _uiState.update {
                    it.copy(isLoading = false, unlockError = R.string.error_inheritance_expired)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        unlockError = R.string.error_unlocking_vault
                    )
                }
            }
        }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(unlockSuccess = false) }
    }
}
