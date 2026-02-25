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
                        .onFailure { _uiState.value = AuthUiState.Error(it.localizedMessage ?: "Credenciales incorrectas o error de autenticación") }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AuthUiState.Error("Tiempo de espera agotado. Verifica tu conexión a internet.")
            } catch (e: IOException) {
                _uiState.value = AuthUiState.Error("No hay conexión a internet. Verifica tu red y vuelve a intentarlo.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Ocurrió un error inesperado al iniciar sesión")
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
                        .onFailure { _uiState.value = AuthUiState.Error(it.localizedMessage ?: "No se pudo registrar la cuenta") }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AuthUiState.Error("Tiempo de espera agotado. Verifica tu conexión a internet.")
            } catch (e: IOException) {
                _uiState.value = AuthUiState.Error("No hay conexión a internet. Verifica tu red y vuelve a intentarlo.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Ocurrió un error inesperado al registrarse")
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
    data class  Error(val message: String) : AuthUiState
}
