package com.perenniasystems.lascito.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents a single Memory (media item) attached to a Legacy.
 *
 * The [contextText] field is MANDATORY by product design — every media item
 * must have a written story explaining its significance.
 *
 * Maps 1:1 to a Firestore document under `legacies/{profileId}/memories`.
 *
 * The no-arg constructor with defaults is required by Firestore's
 * automatic deserialization (`toObject<Memory>()`).
 */
@Keep
@IgnoreExtraProperties
data class Memory(
    val memoryId: String = "",
    val profileId: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val mediaUrl: String = "",
    /** S3 storage class identifier, e.g. "STANDARD", "GLACIER_DEEP_ARCHIVE". */
    val storageClass: String = "STANDARD",
    /** Mandatory: The human story behind this memory. Cannot be blank. */
    val contextText: String = "",
    /** Optional: URL to an audio clip providing additional context. */
    val contextAudioUrl: String? = null,
    val capturedDate: Long = 0L            // Unix timestamp (ms)
)

/** El archivo supera el límite de tamaño que aplican las reglas de Storage. */
class MediaTooLargeException(val isVideo: Boolean) : Exception("Media exceeds the upload size limit")

@Keep
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}
