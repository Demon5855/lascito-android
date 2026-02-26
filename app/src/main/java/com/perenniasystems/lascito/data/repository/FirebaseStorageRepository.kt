package com.perenniasystems.lascito.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.perenniasystems.lascito.data.model.MediaType

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
     * Uploads a large media file (Video/Audio) via URI stream directly to Firebase Storage.
     */
    suspend fun uploadMediaFile(uri: Uri, mediaType: MediaType, profileId: String): Result<String>

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

    override suspend fun uploadMediaFile(uri: Uri, mediaType: MediaType, profileId: String): Result<String> {
        return try {
            val extension = if (mediaType == MediaType.VIDEO) "mp4" else "m4a"
            val folder = if (mediaType == MediaType.VIDEO) "videos" else "audio"
            val uuid = java.util.UUID.randomUUID().toString()
            val path = "memories/$profileId/$folder/$uuid.$extension"

            val storageRef = storage.reference.child(path)
            
            // The Firebase putFile uses an InputStream under the hood
            // Content-Type is inferred by Firebase or from extension, but stream is handled safely.
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
