package com.perenniasystems.lascito.data.repository

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Interface for Firebase Storage operations.
 */
interface FirebaseStorageRepository {
    /**
     * Uploads the provided image [bytes] to the specified [path] in Firebase Storage.
     * @return The download URL of the uploaded image.
     */
    suspend fun uploadImage(bytes: ByteArray, path: String): String

    /**
     * Deletes the image at the given [imageUrl].
     * Tolerates "object-not-found" errors to avoid breaking cascade deletions.
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit>
}

/**
 * Implementation of [FirebaseStorageRepository] using Firebase Storage.
 */
class FirebaseStorageRepositoryImpl(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : FirebaseStorageRepository {

    override suspend fun uploadImage(bytes: ByteArray, path: String): String {
        val storageRef = storage.reference.child(path)
        // Upload the byte array
        storageRef.putBytes(bytes).await()
        // Retrieve and return the download URL
        return storageRef.downloadUrl.await().toString()
    }

    override suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is com.google.firebase.storage.StorageException && e.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                // Fault tolerance: if it's already gone, consider it a success
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }
}
