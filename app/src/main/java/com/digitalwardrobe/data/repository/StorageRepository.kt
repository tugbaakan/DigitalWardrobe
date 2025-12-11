package com.digitalwardrobe.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

/**
 * Repository for Firebase Storage operations.
 */
class StorageRepository(
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val storageRef: StorageReference = firebaseStorage.reference

    /**
     * Upload result sealed class.
     */
    sealed class UploadResult {
        data class Success(val downloadUrl: String) : UploadResult()
        data class Progress(val progress: Int) : UploadResult()
        data class Error(val exception: Exception) : UploadResult()
    }

    /**
     * Upload a body photo to Firebase Storage.
     *
     * @param userId The user ID
     * @param imageUri The URI of the image to upload
     * @return Flow of UploadResult for progress tracking
     */
    fun uploadBodyPhoto(userId: String, imageUri: Uri): Flow<UploadResult> = callbackFlow {
        val fileName = "body_photo_${UUID.randomUUID()}.jpg"
        val photoRef = storageRef.child("users/$userId/body_photos/$fileName")

        val uploadTask = photoRef.putFile(imageUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = ((100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toInt()
            trySend(UploadResult.Progress(progress))
        }.addOnSuccessListener { taskSnapshot ->
            // Get download URL
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                trySend(UploadResult.Success(downloadUri.toString()))
                close()
            }.addOnFailureListener { exception ->
                trySend(UploadResult.Error(exception))
                close()
            }
        }.addOnFailureListener { exception ->
            trySend(UploadResult.Error(exception))
            close()
        }

        awaitClose()
    }

    /**
     * Upload a garment photo to Firebase Storage.
     *
     * @param userId The user ID
     * @param imageUri The URI of the image to upload
     * @return Flow of UploadResult for progress tracking
     */
    fun uploadGarmentPhoto(userId: String, imageUri: Uri): Flow<UploadResult> = callbackFlow {
        val fileName = "garment_${UUID.randomUUID()}.jpg"
        val photoRef = storageRef.child("users/$userId/garments/$fileName")

        val uploadTask = photoRef.putFile(imageUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = ((100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toInt()
            trySend(UploadResult.Progress(progress))
        }.addOnSuccessListener { taskSnapshot ->
            // Get download URL
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                trySend(UploadResult.Success(downloadUri.toString()))
                close()
            }.addOnFailureListener { exception ->
                trySend(UploadResult.Error(exception))
                close()
            }
        }.addOnFailureListener { exception ->
            trySend(UploadResult.Error(exception))
            close()
        }

        awaitClose()
    }

    /**
     * Upload a file from local path to Firebase Storage.
     *
     * @param userId The user ID
     * @param localFile The local file to upload
     * @param folder The folder path (e.g., "body_photos", "garments")
     * @return Flow of UploadResult for progress tracking
     */
    fun uploadFile(userId: String, localFile: File, folder: String): Flow<UploadResult> = callbackFlow {
        val fileName = "${UUID.randomUUID()}_${localFile.name}"
        val fileRef = storageRef.child("users/$userId/$folder/$fileName")

        val uploadTask = fileRef.putFile(Uri.fromFile(localFile))

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = ((100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toInt()
            trySend(UploadResult.Progress(progress))
        }.addOnSuccessListener { taskSnapshot ->
            // Get download URL
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                trySend(UploadResult.Success(downloadUri.toString()))
                close()
            }.addOnFailureListener { exception ->
                trySend(UploadResult.Error(exception))
                close()
            }
        }.addOnFailureListener { exception ->
            trySend(UploadResult.Error(exception))
            close()
        }

        awaitClose()
    }

    /**
     * Delete a file from Firebase Storage.
     *
     * @param downloadUrl The download URL of the file to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(downloadUrl: String): Result<Unit> {
        return try {
            // Extract the path from download URL
            val uri = Uri.parse(downloadUrl)
            val path = uri.pathSegments.dropWhile { it != "o" }.drop(1).joinToString("/")
            val decodedPath = android.net.Uri.decode(path)

            val fileRef = storageRef.child(decodedPath)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user ID or throw exception if not authenticated.
     */
    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
}
