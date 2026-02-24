package com.perenniasystems.lascito.data.repository

/** Contrato de autenticación (se reemplazará con FirebaseAuth en Fase 2). */
interface UserRepository {
    /** Devuelve true si las credenciales son válidas. */
    fun login(email: String, password: String): Boolean
}
