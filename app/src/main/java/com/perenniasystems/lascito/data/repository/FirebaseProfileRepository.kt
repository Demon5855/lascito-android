package com.perenniasystems.lascito.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perenniasystems.lascito.data.model.Profile
import kotlinx.coroutines.tasks.await
import android.util.Log

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
        val memoriesCollection = collection.document(profileId).collection("memories")
        
        // 1. Fetch all memories associated with this profile
        val memoriesSnapshot = memoriesCollection.get().await()
        
        // 2. Iterate through each memory to delete its media and document
        for (document in memoriesSnapshot.documents) {
            val mediaUrl = document.getString("mediaUrl")
            
            // 3. Delete the memory's media from Storage if it exists
            if (!mediaUrl.isNullOrBlank()) {
                val result = storageRepo.deleteImage(mediaUrl)
                if (result.isFailure) {
                    // Log or handle non-object-not-found errors if strictness is required,
                    // but we continue the loop to ensure maximum cleanup effort.
                    val exception = result.exceptionOrNull()
                    if (exception !is com.google.firebase.storage.StorageException || 
                        exception.errorCode != com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                        Log.e("FirebaseProfileRepo", "Warning: Failed to delete media for memory ${document.id}", exception)
                    }
                }
            }
            
            // 4. Delete the memory document from Firestore
            document.reference.delete().await()
        }
        
        // 5. Delete the main profile's cover image from Storage if it exists
        if (!imageUrl.isNullOrBlank()) {
            val result = storageRepo.deleteImage(imageUrl)
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                if (exception !is com.google.firebase.storage.StorageException || 
                    exception.errorCode != com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.e("FirebaseProfileRepo", "Warning: Failed to delete cover image for profile $profileId", exception)
                }
            }
        }
        
        // 6. Delete the main profile document
        collection.document(profileId).delete().await()
    }
}
