package com.perenniasystems.lascito.data.model

/**
 * Represents a Commemorative Profile for a loved one (human or pet).
 *
 * Maps 1:1 to a Firestore document under the `profiles` collection.
 * UUIDs are generated client-side before upload.
 */
data class Profile(
    val profileId: String,
    val creatorUserId: String,
    val fullName: String,
    val profileType: ProfileType,
    val relationship: String,
    val birthDate: Long,             // Unix timestamp (ms)
    val passingDate: Long?,          // null if the profile is "Presente"
    val avatarUrl: String,
    val status: ProfileStatus
)

enum class ProfileType {
    HUMAN,
    PET
}

enum class ProfileStatus {
    /** Editable by the creator. */
    ACTIVE,
    /** Archived — read-only, content is immutable (e.g. migrated to Glacier). */
    READ_ONLY
}
