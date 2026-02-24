package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType

/**
 * Contract for any Profile data source — local mock, Firestore, or DynamoDB.
 * Follows the Repository pattern (SOLID: Dependency Inversion).
 */
interface ProfileRepository {
    suspend fun getProfiles(userId: String): List<Profile>
    suspend fun getProfileById(profileId: String): Profile?
}
