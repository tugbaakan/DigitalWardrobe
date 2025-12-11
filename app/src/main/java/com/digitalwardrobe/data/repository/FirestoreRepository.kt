package com.digitalwardrobe.data.repository

import com.digitalwardrobe.data.models.Garment
import com.digitalwardrobe.data.models.Outfit
import com.digitalwardrobe.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Repository for Firestore database operations.
 */
class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // Collection references
    private val usersCollection = firestore.collection("users")
    private val garmentsCollection = firestore.collection("garments")
    private val outfitsCollection = firestore.collection("outfits")

    // Result sealed class for operations
    sealed class DatabaseResult<out T> {
        data class Success<T>(val data: T) : DatabaseResult<T>()
        data class Error(val exception: Exception) : DatabaseResult<Nothing>()
    }

    // User operations
    suspend fun createUser(user: User): DatabaseResult<String> {
        return try {
            val docRef = usersCollection.document(user.id)
            docRef.set(user).await()
            DatabaseResult.Success(user.id)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun getUser(userId: String): DatabaseResult<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            DatabaseResult.Success(user)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>): DatabaseResult<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            DatabaseResult.Success(Unit)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    // Garment operations
    suspend fun saveGarment(garment: Garment): DatabaseResult<String> {
        return try {
            val docRef = if (garment.id.isEmpty()) {
                // New garment, generate ID
                garmentsCollection.document()
            } else {
                // Existing garment, use provided ID
                garmentsCollection.document(garment.id)
            }

            val garmentWithId = garment.copy(id = docRef.id)
            docRef.set(garmentWithId).await()
            DatabaseResult.Success(docRef.id)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun getGarment(garmentId: String): DatabaseResult<Garment?> {
        return try {
            val document = garmentsCollection.document(garmentId).get().await()
            val garment = document.toObject(Garment::class.java)
            DatabaseResult.Success(garment)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun getUserGarments(userId: String): DatabaseResult<List<Garment>> {
        return try {
            val querySnapshot = garmentsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val garments = querySnapshot.documents.mapNotNull { it.toObject(Garment::class.java) }
            DatabaseResult.Success(garments)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    fun getUserGarmentsFlow(userId: String): Flow<DatabaseResult<List<Garment>>> = callbackFlow {
        val listener = garmentsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DatabaseResult.Error(error))
                    return@addSnapshotListener
                }

                val garments = snapshot?.documents?.mapNotNull { it.toObject(Garment::class.java) } ?: emptyList()
                trySend(DatabaseResult.Success(garments))
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateGarment(garmentId: String, updates: Map<String, Any>): DatabaseResult<Unit> {
        return try {
            garmentsCollection.document(garmentId).update(updates).await()
            DatabaseResult.Success(Unit)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun deleteGarment(garmentId: String): DatabaseResult<Unit> {
        return try {
            garmentsCollection.document(garmentId).delete().await()
            DatabaseResult.Success(Unit)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    // Outfit operations
    suspend fun saveOutfit(outfit: Outfit): DatabaseResult<String> {
        return try {
            val docRef = if (outfit.id.isEmpty()) {
                outfitsCollection.document()
            } else {
                outfitsCollection.document(outfit.id)
            }

            val outfitWithId = outfit.copy(id = docRef.id)
            docRef.set(outfitWithId).await()
            DatabaseResult.Success(docRef.id)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun getOutfit(outfitId: String): DatabaseResult<Outfit?> {
        return try {
            val document = outfitsCollection.document(outfitId).get().await()
            val outfit = document.toObject(Outfit::class.java)
            DatabaseResult.Success(outfit)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    suspend fun getUserOutfits(userId: String): DatabaseResult<List<Outfit>> {
        return try {
            val querySnapshot = outfitsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val outfits = querySnapshot.documents.mapNotNull { it.toObject(Outfit::class.java) }
            DatabaseResult.Success(outfits)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    fun getUserOutfitsFlow(userId: String): Flow<DatabaseResult<List<Outfit>>> = callbackFlow {
        val listener = outfitsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DatabaseResult.Error(error))
                    return@addSnapshotListener
                }

                val outfits = snapshot?.documents?.mapNotNull { it.toObject(Outfit::class.java) } ?: emptyList()
                trySend(DatabaseResult.Success(outfits))
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteOutfit(outfitId: String): DatabaseResult<Unit> {
        return try {
            outfitsCollection.document(outfitId).delete().await()
            DatabaseResult.Success(Unit)
        } catch (e: Exception) {
            DatabaseResult.Error(e)
        }
    }

    /**
     * Get current user ID or throw exception if not authenticated.
     */
    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
}
