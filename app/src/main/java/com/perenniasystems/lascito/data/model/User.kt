package com.perenniasystems.lascito.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa al usuario propietario del santuario digital.
 *
 * Maps 1:1 to a Firestore document under `users/{uid}`.
 *
 * Los campos [legacyGuardian] y [deadManSwitch] son objetos embebidos
 * dentro del mismo documento (no subcolecciones). Las Cápsulas del Tiempo
 * viven en la subcolección `users/{uid}/capsules`.
 *
 * El no-arg constructor con defaults es requerido por la deserialización
 * automática de Firestore (`toObject<User>()`).
 */
@Keep
@IgnoreExtraProperties
data class User(
    /** UID de Firebase Auth — también es el ID del documento en Firestore. */
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    /** Timestamp (ms) de creación de la cuenta. */
    val createdAt: Long = 0L,
    /**
     * Guardián del Legado designado por el usuario.
     * Null si el usuario aún no ha configurado esta función.
     */
    val legacyGuardian: LegacyGuardian? = null,
    /**
     * Configuración del Dead Man's Switch.
     * Null si el usuario nunca ha activado esta función.
     * Cuando es null, la función se considera inactiva.
     */
    val deadManSwitch: DeadManSwitch? = null
)
