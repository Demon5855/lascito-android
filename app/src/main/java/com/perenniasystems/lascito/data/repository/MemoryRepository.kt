package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Memory

/**
 * Contract for fetching [Memory] objects from any data source.
 * Phase 2: implement with Firestore subcollection `profiles/{id}/memories`.
 */
interface MemoryRepository {
    suspend fun getMemoriesByProfile(profileId: String): List<Memory>
}
