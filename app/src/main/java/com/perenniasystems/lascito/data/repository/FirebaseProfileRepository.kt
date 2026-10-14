package com.perenniasystems.lascito.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Profile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firestore implementation of [ProfileRepository].
 *
 * Collection: `profiles`
 * Document ID = [Profile.profileId]
 *
 * Queries are filtered by `creatorUserId` to scope profiles to the
 * currently authenticated user.
 */
class FirebaseProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storageRepo: FirebaseStorageRepository = FirebaseStorageRepositoryImpl()
) : ProfileRepository {

    private val collection get() = db.collection("profiles")

    override suspend fun getProfiles(userId: String): List<Profile> =
        withTimeout(10_000L) {
            collection
                .whereEqualTo("creatorUserId", userId)
                .orderBy("fullName", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Profile::class.java)
        }

    override suspend fun getProfileById(profileId: String): Profile? =
        withTimeout(10_000L) {
            collection
                .document(profileId)
                .get()
                .await()
                .toObject(Profile::class.java)
        }

    override suspend fun save(profile: Profile): String = withTimeout(10_000L) {
        val docRef = if (profile.profileId.isBlank()) {
            collection.document()  // auto-generate ID
        } else {
            collection.document(profile.profileId)
        }
        val finalProfile = if (profile.profileId.isBlank()) {
            profile.copy(profileId = docRef.id)
        } else {
            profile
        }
        docRef.set(finalProfile).await()
        docRef.id
    }

    override suspend fun deleteProfile(profileId: String, imageUrl: String?) {
        val memoriesCollection = collection.document(profileId).collection("memories")

        // 1. Fetch all memories associated with this profile
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

        // 5. Delete the main profile's cover image from Storage if it exists
        if (!imageUrl.isNullOrBlank()) {
            // Best effort, same as the memories above.
            storageRepo.deleteImage(imageUrl)
        }

        // 6. Delete the main profile document
        withTimeout(10_000L) { collection.document(profileId).delete().await() }
    }
}
