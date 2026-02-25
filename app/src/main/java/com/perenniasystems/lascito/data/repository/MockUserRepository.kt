package com.perenniasystems.lascito.data.repository

/**
 * Implementación mock para Phase 1.
 * Fase 2: sustituir por [FirebaseAuthRepository].
 *
 * Usuario administrador de prueba:
 *   Nombre:     Administrador
 *   Correo:     admin@perenniasys.com
 *   Contraseña: Demo@1234
 */
object MockUserRepository : UserRepository {

    private data class MockUser(
        val uid:         String,
        val displayName: String,
        val email:       String,
        val password:    String
    )

    private val users = listOf(
        MockUser(
            uid         = "mock-user-123",
            displayName = "Administrador",
            email       = "admin@perenniasys.com",
            password    = "Demo@1234"
        )
    )

    private var currentUid: String? = null

    override suspend fun login(email: String, password: String): Result<String> {
        val user = users.find {
            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
        }
        return if (user != null) {
            currentUid = user.uid
            Result.success(user.uid)
        } else {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<String> {
        currentUid = "user-mock-new"
        return Result.success("user-mock-new")
    }

    override fun currentUserId(): String? = currentUid

    override fun logout() { currentUid = null }
}
