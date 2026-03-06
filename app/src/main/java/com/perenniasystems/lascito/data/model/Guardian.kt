package com.perenniasystems.lascito.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estado de la invitación enviada al Guardián del Legado.
 */
@Keep
enum class GuardianStatus {
    /** El guardián aún no ha respondido a la invitación. */
    PENDING,
    /** El guardián aceptó el rol y su acceso ha sido activado. */
    ACCEPTED,
    /** El guardián rechazó el rol. */
    REJECTED
}

/**
 * Estado del proceso de revelación del legado digital.
 */
@Keep
enum class UnlockStatus {
    /** El santuario está activo y el propietario sigue generando Pruebas de Vida. */
    LOCKED,
    /** El período de espera ha comenzado; aún reversible si el propietario responde. */
    UNLOCKING,
    /** El período de espera expiró. El Guardián tiene acceso completo. */
    UNSEALED
}

// ─────────────────────────────────────────────────────────────────────────────
// Data Classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representa al Guardián del Legado designado por el usuario.
 *
 * Se almacena como objeto embebido dentro del documento `users/{uid}`.
 * El no-arg constructor con defaults es requerido por la deserialización
 * automática de Firestore (`toObject<User>()`).
 */
@Keep
@IgnoreExtraProperties
data class LegacyGuardian(
    /** Correo electrónico del guardián para enviar la invitación. */
    val email: String? = null,
    /** Nombre para mostrar en las comunicaciones y en la UI del guardián. */
    val name: String? = null,
    /** Estado actual de la aceptación del rol. */
    val status: GuardianStatus = GuardianStatus.PENDING,
    /**
     * Hash del PIN de acceso temporal que el guardián usará para autenticarse
     * sin necesidad de una cuenta Firebase propia.
     * Almacenar SIEMPRE como hash (bcrypt/SHA-256), nunca en texto plano.
     */
    val accessPinHash: String? = null,
    /** Timestamp (ms) de la última vez que el usuario modificó su guardián.
     * Utilizado para el Cooldown Anti-Spam (ej: 7 días).*/
    val lastGuardianUpdatedAt: Long? = null
)

/**
 * Configuración del Dead Man's Switch (Interruptor de Hombre Muerto).
 *
 * Se almacena como objeto embebido dentro del documento `users/{uid}`.
 * Controla cuánto tiempo de inactividad se tolera antes de iniciar
 * el proceso de revelación del legado.
 */
@Keep
@IgnoreExtraProperties
data class DeadManSwitch(
    /** Indica si el interruptor está activo para este usuario. */
    val isActive: Boolean = false,
    /**
     * Días de inactividad necesarios para considerar que el interruptor
     * debe activarse e iniciar el proceso de revelación.
     */
    val inactivityThresholdDays: Int = 90,
    /**
     * Timestamp (ms) de la última Prueba de Vida registrada.
     * Se actualiza cada vez que el usuario abre la app o lo hace
     * manualmente desde Ajustes.
     */
    val lastProofOfLife: Long = 0L,
    /** Estado actual del proceso de revelación. */
    val unlockStatus: UnlockStatus = UnlockStatus.LOCKED,
    /** Timestamp (ms) en que se inició el período de espera de revelación. */
    val unlockStartedAt: Long? = null,
    /**
     * Timestamp (ms) calculado en que el período de espera expira y el
     * santuario pasa a estado UNSEALED automáticamente.
     */
    val unlockReadyAt: Long? = null
)
