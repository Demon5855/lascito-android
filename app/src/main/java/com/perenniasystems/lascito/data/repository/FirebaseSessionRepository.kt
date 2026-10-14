package com.perenniasystems.lascito.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSessionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : SessionRepository {

    private val _authState = MutableStateFlow(auth.currentUser.toAuthState())
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // El repositorio es @Singleton: el listener vive lo mismo que el proceso.
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser.toAuthState()
        }
    }

    override val currentUser: SessionUser?
        get() = auth.currentUser?.toSessionUser()

    override suspend fun reloadUser(): Result<SessionUser> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
        user.reload().await()
        val refreshed = (auth.currentUser ?: user).toSessionUser()
        _authState.value = AuthState.SignedIn(refreshed)
        refreshed
    }

    override suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        user.sendEmailVerification().await()
    }

    override fun signOut() {
        auth.signOut()
    }

    /**
     * CRÍTICO: usa [update] en lugar de [get] + write para evitar descargar
     * el documento completo. Si el campo aun no existe (usuario nuevo que
     * nunca configuró el switch), [update] lanzará una excepción → fallback
     * con [set] + merge para crear la estructura mínima inicial.
     */
    override suspend fun registerProofOfLife(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No hay un usuario autenticado.")
        val now = System.currentTimeMillis()
        val userDoc = db.collection("users").document(uid)
        withTimeout(10_000L) {
            try {
                // Camino óptimo: actualización atómica de campo único (O(1))
                userDoc.update("deadManSwitch.lastProofOfLife", now).await()
            } catch (e: Exception) {
                // Fallback: el mapa deadManSwitch aún no existe en el documento.
                // Creamos la estructura mínima vía merge para no pisar otros campos.
                userDoc
                    .set(mapOf("deadManSwitch" to mapOf("lastProofOfLife" to now)), SetOptions.merge())
                    .await()
            }
        }
    }

    private fun FirebaseUser.toSessionUser() = SessionUser(
        uid = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified
    )

    private fun FirebaseUser?.toAuthState(): AuthState =
        this?.let { AuthState.SignedIn(it.toSessionUser()) } ?: AuthState.SignedOut
}
