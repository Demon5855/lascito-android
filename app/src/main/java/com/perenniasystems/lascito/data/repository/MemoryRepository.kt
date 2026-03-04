package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Memory

/**
 * Contract for fetching and persisting [Memory] objects from any data source.
 * Phase 2: implemented with Firestore subcollection `profiles/{id}/memories`.
 */
interface MemoryRepository {
    suspend fun getMemoriesByProfile(profileId: String): List<Memory>

    /**
     * Fetches a single [Memory] by its ID without downloading the entire collection.
     * Returns null if the document does not exist or the snapshot is empty.
     */
    suspend fun getMemoryById(profileId: String, memoryId: String): Memory?

    /** Persist a new memory. Returns the generated document ID. */
    suspend fun save(memory: Memory): String

    /** Deletes the memory document and its associated media from Storage. */
    suspend fun deleteMemory(memoryId: String, profileId: String, mediaUrl: String?)
}
