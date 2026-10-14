package com.perenniasystems.lascito.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Memory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firestore implementation of [MemoryRepository].
 *
 * Subcollection: `profiles/{profileId}/memories`
 * Document ID = [Memory.memoryId]
 */
@Singleton
class FirebaseMemoryRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val storageRepo: FirebaseStorageRepository
) : MemoryRepository {

    private fun memoriesCol(profileId: String) =
        db.collection("profiles").document(profileId).collection("memories")

    override suspend fun getMemoriesByProfile(profileId: String): List<Memory> =
        withTimeout(10_000L) {
            memoriesCol(profileId)
                .orderBy("capturedDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Memory::class.java)
        }

    /**
     * Single-document fetch: accede directamente al documento por ID (O(1))
     * en lugar de descargar toda la colección (O(N)).
     * Devuelve null si el documento no existe o si el snapshot está vacío.
     */
    override suspend fun getMemoryById(profileId: String, memoryId: String): Memory? =
        withTimeout(10_000L) {
            val snapshot = memoriesCol(profileId)
                .document(memoryId)
                .get()
                .await()
            if (snapshot.exists()) snapshot.toObject(Memory::class.java) else null
        }

    override suspend fun save(memory: Memory): String = withTimeout(10_000L) {
        val col = memoriesCol(memory.profileId)
        val docRef = if (memory.memoryId.isBlank()) {
            col.document()
        } else {
            col.document(memory.memoryId)
        }
        val finalMemory = if (memory.memoryId.isBlank()) {
            memory.copy(memoryId = docRef.id)
        } else {
            memory
        }
        docRef.set(finalMemory).await()
        docRef.id
    }

    override suspend fun deleteMemory(memoryId: String, profileId: String, mediaUrl: String?) {
        // Cascade step 1: Delete media if it exists
        if (!mediaUrl.isNullOrBlank()) {
            val result = storageRepo.deleteImage(mediaUrl)
            if (result.isFailure) {
                // Halt document deletion if media deletion fails (unless object-not-found which is tolerated)
                val exception = result.exceptionOrNull()
                throw exception ?: Exception("Failed to delete media for memory cascade")
            }
        }

        // Cascade step 2: Delete document
        withTimeout(10_000L) {
            memoriesCol(profileId).document(memoryId).delete().await()
        }
    }
}
