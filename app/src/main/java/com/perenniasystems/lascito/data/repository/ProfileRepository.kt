package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Profile

/**
 * Contract for any Profile data source — local mock, Firestore, or DynamoDB.
 * Follows the Repository pattern (SOLID: Dependency Inversion).
 */
interface ProfileRepository {
    suspend fun getProfiles(userId: String): List<Profile>
    suspend fun getProfileById(profileId: String): Profile?
    /** Persist a new profile. Returns the generated document ID. */
    suspend fun save(profile: Profile): String
    
    /** Deletes the profile and its associated image from Storage. */
    suspend fun deleteProfile(profileId: String, imageUrl: String?)
}
