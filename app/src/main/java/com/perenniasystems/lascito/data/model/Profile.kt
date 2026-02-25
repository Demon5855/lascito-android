package com.perenniasystems.lascito.data.model

/**
 * Represents a Commemorative Profile for a loved one (human or pet).
 *
 * Maps 1:1 to a Firestore document under the `profiles` collection.
 * UUIDs are generated client-side before upload.
 *
 * The no-arg constructor with defaults is required by Firestore's
 * automatic deserialization (`toObject<Profile>()`).
 */
data class Profile(
    val profileId: String = "",
    val creatorUserId: String = "",
    val fullName: String = "",
    val profileType: ProfileType = ProfileType.HUMAN,
    val relationship: String = "",
    val birthDate: Long = 0L,             // Unix timestamp (ms)
    val passingDate: Long? = null,        // null if the profile is "Presente"
    val avatarUrl: String = "",
    val status: ProfileStatus = ProfileStatus.ACTIVE
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
