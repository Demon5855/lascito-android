package com.perenniasystems.lascito.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Memory
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of [MemoryRepository].
 *
 * Subcollection: `profiles/{profileId}/memories`
 * Document ID = [Memory.memoryId]
 */
class FirebaseMemoryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MemoryRepository {

    private fun memoriesCol(profileId: String) =
        db.collection("profiles").document(profileId).collection("memories")

    override suspend fun getMemoriesByProfile(profileId: String): List<Memory> =
        memoriesCol(profileId)
            .orderBy("capturedDate", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Memory::class.java)

    override suspend fun save(memory: Memory): String {
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
        return docRef.id
    }
}
