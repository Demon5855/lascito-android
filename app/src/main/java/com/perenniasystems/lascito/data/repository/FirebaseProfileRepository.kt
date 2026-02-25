package com.perenniasystems.lascito.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Profile
import kotlinx.coroutines.tasks.await

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
        collection
            .whereEqualTo("creatorUserId", userId)
            .orderBy("fullName", Query.Direction.ASCENDING)
            .get()
            .await()
            .toObjects(Profile::class.java)

    override suspend fun getProfileById(profileId: String): Profile? =
        collection
            .document(profileId)
            .get()
            .await()
            .toObject(Profile::class.java)

    override suspend fun save(profile: Profile): String {
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
        return docRef.id
    }

    override suspend fun deleteProfile(profileId: String, imageUrl: String?) {
        // Cascade step 1: Delete image if it exists
        if (!imageUrl.isNullOrBlank()) {
            val result = storageRepo.deleteImage(imageUrl)
            if (result.isFailure) {
                // If it's a real failure (not object-not-found), you could throw it here
                // to halt the document deletion, depending on strictness.
                // The Architect requirement strictly states:
                // "If image deletion is successful (or absent), call collection.document(id).delete()"
                val exception = result.exceptionOrNull()
                throw exception ?: Exception("Failed to delete image for profile cascade")
            }
        }
        
        // Cascade step 2: Delete document
        collection.document(profileId).delete().await()
    }
}
