package com.perenniasystems.lascito.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Usuario de la sesión actual, sin tipos de Firebase. */
data class SessionUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val user: SessionUser) : AuthState
}

/**
 * Única fuente de la sesión. Fuera de `data/` nadie consulta Firebase Auth
 * directamente: se lee [authState] o [currentUser].
 */
interface SessionRepository {

    /** Estado de autenticación. Tiene valor inicial síncrono (sin parpadeos al arrancar). */
    val authState: StateFlow<AuthState>

    /** Usuario actual en este instante, o null si no hay sesión. */
    val currentUser: SessionUser?

    /**
     * Recarga el usuario desde el servidor para conocer el estado de verificación
     * de correo más reciente. Falla si la sesión caducó o no hay usuario.
     */
    suspend fun reloadUser(): Result<SessionUser>

    /** Reenvía el correo de verificación al usuario actual. */
    suspend fun resendVerificationEmail(): Result<Unit>

    fun signOut()

    /**
     * Registra una Prueba de Vida: escribe el timestamp actual en
     * `deadManSwitch.lastProofOfLife` sin modificar el resto de la config.
     */
    suspend fun registerProofOfLife(): Result<Unit>
}
