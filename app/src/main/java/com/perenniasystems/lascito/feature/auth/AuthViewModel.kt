package com.perenniasystems.lascito.feature.auth

import com.perenniasystems.lascito.data.repository.SessionRepository
import com.perenniasystems.lascito.data.error.DataErrors
import com.perenniasystems.lascito.data.error.DataErrorKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.core.common.ErrorHandler
import java.io.IOException

/**
 * ViewModel for authentication (login + register).
 *
 * Shared between LoginScreen and RegisterScreen via the NavGraph
 * (scoped to the navigation back-stack).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo:    UserRepository,
    private val session: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Si el usuario ya tiene una sesión activa al iniciar la app,
        // registramos silenciosamente una Prueba de Vida para retrasar
        // el Dead Man's Switch sin bloquear la navegación.
        if (session.currentUser != null) {
            sendProofOfLife()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withTimeout(15_000L) {
                    repo.login(email, password)
                        .onSuccess {
                            // El login exitoso es en sí una Prueba de Vida.
                            sendProofOfLife()
                            _uiState.value = AuthUiState.Success
                        }
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
        session.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Registra una Prueba de Vida de forma silenciosa (fire-and-forget).
     *
     * - Se ejecuta en [Dispatchers.IO] para no bloquear el hilo principal.
     * - Un fallo de red NO se propaga a la UI.
     *   La Prueba de Vida es una mejora de calidad, nunca un bloqueante crítico.
     */
    private fun sendProofOfLife() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                session.registerProofOfLife()
            } catch (e: Exception) {
                // Fallo silencioso: sin internet o usuario sin switch configurado.
                // No mostramos error en la UI; el latido se enviará en la próxima sesión.
            }
        }
    }

    private var lastResetTime = 0L

    fun resetPassword(email: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastResetTime < 60_000L) {
            val remainingSeconds = (60_000L - (currentTime - lastResetTime)) / 1000
            _uiState.value = AuthUiState.Error(
                // "Espera %1$d segundos antes de reintentar"
                R.string.error_reset_cooldown, 
                listOf(remainingSeconds.toInt())
            )
            return
        }
        lastResetTime = currentTime

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withTimeout(15_000L) {
                    repo.sendPasswordReset(email).getOrThrow()
                }
                _uiState.value = AuthUiState.ResetEmailSent
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    if (DataErrors.classify(e) == DataErrorKind.INVALID_USER) R.string.error_login
                    else ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }
}

// ── Sealed UI State ────────────────────────────────────────────────────────────
sealed interface AuthUiState {
    data object Idle    : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data object ErrorUnverified : AuthUiState
    data object ResetEmailSent : AuthUiState
    data class  Error(@StringRes val messageResId: Int, val formatArgs: List<Any> = emptyList()) : AuthUiState
}
