package com.perenniasystems.lascito.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

// ─────────────────────────────────────────────────────────────────────────────
// Enum
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Define cuándo se revelará el contenido de la Cápsula del Tiempo.
 */
@Keep
enum class TriggerType {
    /**
     * La cápsula se revela inmediatamente cuando el santuario
     * pasa a estado UNSEALED (post-mortem inmediato).
     */
    IMMEDIATE_POST_MORTEM,
    /**
     * La cápsula se revela en una fecha futura específica,
     * independientemente del estado del Dead Man's Switch.
     * Requiere [TimeCapsule.scheduledUnlockDate].
     */
    POST_MORTEM_DATE
}

// ─────────────────────────────────────────────────────────────────────────────
// Data Class
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representa una Cápsula del Tiempo: un mensaje o medio que el usuario
 * deja para que sea revelado en el futuro bajo condiciones específicas.
 *
 * Estructura en Firestore: `users/{uid}/capsules/{capsuleId}`
 *
 * El no-arg constructor con defaults es requerido por la deserialización
 * automática de Firestore (`toObject<TimeCapsule>()`).
 */
@Keep
@IgnoreExtraProperties
data class TimeCapsule(
    /** Identificador único generado en cliente (UUID). */
    val capsuleId: String = "",
    /**
     * Perfil al que está vinculada esta cápsula.
     * Puede ser null si la cápsula es de carácter general (no vinculada
     * a un perfil específico, ej. un mensaje a la familia).
     */
    val profileId: String? = null,
    /** Título descriptivo visible para el creador antes del unlock. */
    val title: String = "",
    /**
     * Texto del mensaje / carta que se revelará al Guardián.
     * Null si el contenido es exclusivamente multimedia.
     */
    val content: String? = null,
    /**
     * URL en Firebase Storage del video, audio o imagen adjunto.
     * Null si la cápsula es solo texto.
     */
    val mediaUrl: String? = null,
    /** Tipo de medio adjunto. Ignorado si [mediaUrl] es null. */
    val mediaType: MediaType = MediaType.IMAGE,
    /** Condición que debe cumplirse para revelar esta cápsula. */
    val triggerType: TriggerType = TriggerType.IMMEDIATE_POST_MORTEM,
    /**
     * Timestamp (ms) de la fecha futura en que se revelará la cápsula.
     * Solo relevante cuando [triggerType] == [TriggerType.POST_MORTEM_DATE].
     */
    val scheduledUnlockDate: Long? = null,
    /**
     * Flag visual: true mientras el propietario está vivo y la cápsula
     * no ha sido revelada. La UI muestra el contenido como "sellado".
     */
    val isLocked: Boolean = true,
    /** Timestamp (ms) de creación de la cápsula. */
    val createdAt: Long = 0L
)
