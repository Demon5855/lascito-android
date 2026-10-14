package com.perenniasystems.lascito.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents a Commemorative Legacy for a loved one (human or pet).
 *
 * Maps 1:1 to a Firestore document under the `legacies` collection.
 * UUIDs are generated client-side before upload.
 *
 * The no-arg constructor with defaults is required by Firestore's
 * automatic deserialization (`toObject<Legacy>()`).
 */
@Keep
@IgnoreExtraProperties
data class Legacy(
    val profileId: String = "",
    val creatorUserId: String = "",
    val fullName: String = "",
    val profileType: LegacyType = LegacyType.HUMAN,
    val relationship: String = "",
    val birthDate: Long = 0L,             // Unix timestamp (ms)
    val passingDate: Long? = null,        // null if the legacy is "Presente"
    val avatarUrl: String = "",
    val status: LegacyStatus = LegacyStatus.ACTIVE
)

@Keep
enum class LegacyType {
    HUMAN,
    PET
}

@Keep
enum class LegacyStatus {
    /** Editable by the creator. */
    ACTIVE,
    /** Archived — read-only, content is immutable (e.g. migrated to Glacier). */
    READ_ONLY
}
