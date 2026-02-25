package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.repository.FirebaseAuthRepository
import com.perenniasystems.lascito.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.util.ErrorHandler
import java.io.IOException

/**
 * ViewModel for authentication (login + register).
 *
 * Shared between LoginScreen and RegisterScreen via the NavGraph
 * (scoped to the navigation back-stack).
 */
class AuthViewModel(
    private val repo: UserRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withTimeout(15_000L) {
                    repo.login(email, password)
                        .onSuccess { _uiState.value = AuthUiState.Success }
                        .onFailure { t ->
                            _uiState.value = AuthUiState.Error(
                                ErrorHandler.mapExceptionToResId(t as? Exception ?: Exception(t))
                            )
                        }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AuthUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withTimeout(15_000L) {
                    repo.register(name, email, password)
                        .onSuccess { _uiState.value = AuthUiState.Success }
                        .onFailure { t ->
                            _uiState.value = AuthUiState.Error(
                                ErrorHandler.mapExceptionToResId(t as? Exception ?: Exception(t))
                            )
                        }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AuthUiState.Error(R.string.error_network)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorHandler.mapExceptionToResId(e))
            }
        }
    }

    fun logout() {
        repo.logout()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

// ── Sealed UI State ────────────────────────────────────────────────────────────
sealed interface AuthUiState {
    data object Idle    : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class  Error(@StringRes val messageResId: Int) : AuthUiState
}
