package com.perenniasystems.lascito.data.repository

/**
 * Implementación mock para Phase 1.
 * Fase 2: sustituir por FirebaseAuth.authenticate().
 *
 * Usuario administrador de prueba:
 *   Nombre:     Administrador
 *   Correo:     admin@perenniasys.com
 *   Contraseña: Demo@1234
 */
object MockUserRepository : UserRepository {

    private data class MockUser(
        val displayName: String,
        val email:       String,
        val password:    String   // plaintext solo para mock; en prod → hash o Firebase
    )

    private val users = listOf(
        MockUser(
            displayName = "Administrador",
            email       = "admin@perenniasys.com",
            password    = "password"  // meets strength rules: upper, lower, digit, symbol
        )
    )

    override fun login(email: String, password: String): Boolean =
        users.any { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }
}
