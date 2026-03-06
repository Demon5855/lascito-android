package com.perenniasystems.lascito.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
     * Optionally reports progress from 0.0 to 1.0.
     */
    suspend fun uploadMediaFile(uri: Uri, mediaType: MediaType, profileId: String, onProgress: (Float) -> Unit = {}): Result<String>

    /**
     * Uploads time capsule media keeping the exact path format: capsules/{userId}/{capsuleId}.{extension}
     */
    suspend fun uploadCapsuleMedia(uri: Uri, userId: String, capsuleId: String, extension: String, onProgress: (Float) -> Unit = {}): Result<String>


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

    override suspend fun uploadMediaFile(uri: Uri, mediaType: MediaType, profileId: String, onProgress: (Float) -> Unit): Result<String> {
        return try {
            val extension = if (mediaType == MediaType.VIDEO) "mp4" else "m4a"
            val folder = if (mediaType == MediaType.VIDEO) "videos" else "audio"
            val uuid = java.util.UUID.randomUUID().toString()
            val path = "memories/$profileId/$folder/$uuid.$extension"

            val storageRef = storage.reference.child(path)
            
            val uploadTask = storageRef.putFile(uri)

            uploadTask.addOnProgressListener { snapshot ->
                val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                onProgress(progress)
            }

            // suspendCancellableCoroutine garantiza que si la corrutina padre es cancelada
            // (ej. timeout del ViewModel), la tarea de Firebase se cancela físicamente.
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    uploadTask.cancel()
                }
                uploadTask
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { e -> continuation.resumeWithException(e) }
            }

            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadCapsuleMedia(uri: Uri, userId: String, capsuleId: String, extension: String, onProgress: (Float) -> Unit): Result<String> {
        return try {
            val path = "capsules/$userId/$capsuleId.$extension"
            val storageRef = storage.reference.child(path)
            
            val uploadTask = storageRef.putFile(uri)

            uploadTask.addOnProgressListener { snapshot ->
                val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                onProgress(progress)
            }

            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { uploadTask.cancel() }
                uploadTask
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { e -> continuation.resumeWithException(e) }
            }

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
