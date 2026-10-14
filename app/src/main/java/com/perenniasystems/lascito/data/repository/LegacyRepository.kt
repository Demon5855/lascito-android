package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Legacy

/**
 * Contract for any Legacy data source — local mock, Firestore, or DynamoDB.
 * Follows the Repository pattern (SOLID: Dependency Inversion).
 */
interface LegacyRepository {
    suspend fun getLegacies(userId: String): List<Legacy>
    suspend fun getLegacyById(profileId: String): Legacy?
    /** Persist a new legacy. Returns the generated document ID. */
    suspend fun save(legacy: Legacy): String
    
    /** Deletes the legacy and its associated image from Storage. */
    suspend fun deleteLegacy(profileId: String, imageUrl: String?)
}
