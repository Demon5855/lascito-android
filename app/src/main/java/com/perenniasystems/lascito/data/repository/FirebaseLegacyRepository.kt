package com.perenniasystems.lascito.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Legacy
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firestore implementation of [LegacyRepository].
 *
 * Collection: `legacies`
 * Document ID = [Legacy.profileId]
 *
 * Queries are filtered by `creatorUserId` to scope legacies to the
 * currently authenticated user.
 */
@Singleton
class FirebaseLegacyRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val storageRepo: FirebaseStorageRepository
) : LegacyRepository {

    private val collection get() = db.collection("profiles")

    override suspend fun getLegacies(userId: String): List<Legacy> =
        withTimeout(10_000L) {
            collection
                .whereEqualTo("creatorUserId", userId)
                .orderBy("fullName", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Legacy::class.java)
        }

    override suspend fun getLegacyById(profileId: String): Legacy? =
        withTimeout(10_000L) {
            collection
                .document(profileId)
                .get()
                .await()
                .toObject(Legacy::class.java)
        }

    override suspend fun save(legacy: Legacy): String = withTimeout(10_000L) {
        val docRef = if (legacy.profileId.isBlank()) {
            collection.document()  // auto-generate ID
        } else {
            collection.document(legacy.profileId)
        }
        val finalLegacy = if (legacy.profileId.isBlank()) {
            legacy.copy(profileId = docRef.id)
        } else {
            legacy
        }
        docRef.set(finalLegacy).await()
        docRef.id
    }

    override suspend fun deleteLegacy(profileId: String, imageUrl: String?) {
        val memoriesCollection = collection.document(profileId).collection("memories")

        // 1. Fetch all memories associated with this legacy
        val memoriesSnapshot = withTimeout(10_000L) { memoriesCollection.get().await() }

        // 2. Iterate through each memory to delete its media and document
        for (document in memoriesSnapshot.documents) {
            val mediaUrl = document.getString("mediaUrl")

            // 3. Delete the memory's media from Storage if it exists
            if (!mediaUrl.isNullOrBlank()) {
                // A failed media deletion does not stop the loop: we continue to
                // ensure maximum cleanup effort.
                storageRepo.deleteImage(mediaUrl)
            }

            // 4. Delete the memory document from Firestore
            withTimeout(10_000L) { document.reference.delete().await() }
        }

        // 5. Delete the main legacy's cover image from Storage if it exists
        if (!imageUrl.isNullOrBlank()) {
            // Best effort, same as the memories above.
            storageRepo.deleteImage(imageUrl)
        }

        // 6. Delete the main legacy document
        withTimeout(10_000L) { collection.document(profileId).delete().await() }
    }
}
