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
}
