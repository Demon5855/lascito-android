package com.perenniasystems.lascito.data.repository

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.perenniasystems.lascito.data.model.MediaTooLargeException
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

    private companion object {
        // Keep in sync with storage.rules
        const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
        const val MAX_AUDIO_BYTES = 50L * 1024 * 1024
        const val MAX_VIDEO_BYTES = 200L * 1024 * 1024
    }

    override suspend fun uploadImage(bytes: ByteArray, path: String): String {
        if (bytes.size >= MAX_IMAGE_BYTES) throw MediaTooLargeException(isVideo = false)
        val storageRef = storage.reference.child(path)
        // ImageCompressor always produces WEBP; storage.rules require an image content type
        storageRef.putBytes(bytes, metadataOf("image/webp")).await()
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

            val contentType = if (mediaType == MediaType.VIDEO) {
                resolveContentType(uri)?.takeIf { it.startsWith("video/") } ?: "video/mp4"
            } else {
                "audio/mp4"
            }
            ensureWithinLimit(uri, contentType)
            val uploadTask = storageRef.putFile(uri, metadataOf(contentType))

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

            val contentType = resolveContentType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ensureWithinLimit(uri, contentType)
            val uploadTask = if (contentType != null) {
                storageRef.putFile(uri, metadataOf(contentType))
            } else {
                storageRef.putFile(uri)
            }

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

    private fun metadataOf(contentType: String): StorageMetadata =
        StorageMetadata.Builder().setContentType(contentType).build()

    /** MIME type reported by the ContentResolver; null for `file://` URIs. */
    private fun resolveContentType(uri: Uri): String? =
        storage.app.applicationContext.contentResolver.getType(uri)

    /** Fails fast with [MediaTooLargeException] instead of letting storage.rules reject the upload. */
    private fun ensureWithinLimit(uri: Uri, contentType: String?) {
        val limit = when {
            contentType == null -> return
            contentType.startsWith("video/") -> MAX_VIDEO_BYTES
            contentType.startsWith("audio/") -> MAX_AUDIO_BYTES
            else -> MAX_IMAGE_BYTES
        }
        val size = runCatching {
            storage.app.applicationContext.contentResolver
                .openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull() ?: return
        if (size >= limit) throw MediaTooLargeException(isVideo = contentType.startsWith("video/"))
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
