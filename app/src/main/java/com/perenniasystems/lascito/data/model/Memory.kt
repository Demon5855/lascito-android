package com.perenniasystems.lascito.data.model

/**
 * Represents a single Memory (media item) attached to a Profile.
 *
 * The [contextText] field is MANDATORY by product design — every media item
 * must have a written story explaining its significance.
 *
 * Maps 1:1 to a Firestore document under `profiles/{profileId}/memories`.
 */
data class Memory(
    val memoryId: String,
    val profileId: String,
    val mediaType: MediaType,
    val mediaUrl: String,
    /** S3 storage class identifier, e.g. "STANDARD", "GLACIER_DEEP_ARCHIVE". */
    val storageClass: String,
    /** Mandatory: The human story behind this memory. Cannot be blank. */
    val contextText: String,
    /** Optional: URL to an audio clip providing additional context. */
    val contextAudioUrl: String?,
    val capturedDate: Long            // Unix timestamp (ms)
)

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}
