package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.InvalidPinException
import com.perenniasystems.lascito.data.model.LegacyBlockedException
import com.perenniasystems.lascito.data.model.LegacyVault
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.util.ValidationRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClaimLegacyUiState(
    val pin: String = "",
    val pinError: Int? = null,
    val isLoading: Boolean = false,
    val isUnlockEnabled: Boolean = false,
    val unlockError: Int? = null,
    val unlockSuccess: Boolean = false,
    val attemptsRemaining: Int = 3,
    val isLocked: Boolean = false,
    val legacyVault: LegacyVault? = null
)

class ClaimLegacyViewModel(
    private val guardianRepository: GuardianRepository = com.perenniasystems.lascito.data.repository.FirebaseGuardianRepository(),
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClaimLegacyUiState())
    val uiState: StateFlow<ClaimLegacyUiState> = _uiState.asStateFlow()

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
        val isValid = state.pinError == null
        _uiState.update {
            it.copy(isUnlockEnabled = isValid && !state.isLoading)
        }
    }

    fun claimVault() {
        val state = _uiState.value
        if (!state.isUnlockEnabled) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    unlockError = null
                )
            }

            try {
                val legacyVault = guardianRepository.claimInheritance(
                    pin = state.pin
                ).getOrThrow()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        unlockSuccess = true,
                        legacyVault = legacyVault
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
            } catch (e: LegacyBlockedException) {
                // Backend dictamina que se acabaron los intentos
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLocked = true,
                        attemptsRemaining = 0,
                        unlockError = R.string.error_too_many_attempts
                    )
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
