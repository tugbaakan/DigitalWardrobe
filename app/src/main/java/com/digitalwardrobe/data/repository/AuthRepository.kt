package com.digitalwardrobe.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository handling Firebase Authentication operations.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /**
     * Current authenticated user.
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Check if user is logged in.
     */
    val isLoggedIn: Boolean
        get() = currentUser != null

    /**
     * Flow that emits authentication state changes.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed: User is null"))
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Create a new user with email and password.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                Result.success(user)
            } ?: Result.failure(Exception("Sign up failed: User is null"))
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Update user's display name.
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            currentUser?.let { user ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                Result.success(Unit)
            } ?: Result.failure(Exception("No user logged in"))
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Reload user data from Firebase.
     */
    suspend fun reloadUser(): Result<Unit> {
        return try {
            currentUser?.reload()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Map Firebase exceptions to user-friendly error messages.
     */
    private fun mapFirebaseException(exception: Exception): Exception {
        val message = when {
            exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
            exception.message?.contains("INVALID_EMAIL") == true ||
            exception.message?.contains("invalid-credential") == true ->
                "Invalid email or password"
            
            exception.message?.contains("EMAIL_EXISTS") == true ||
            exception.message?.contains("email-already-in-use") == true ->
                "This email is already registered"
            
            exception.message?.contains("WEAK_PASSWORD") == true ||
            exception.message?.contains("weak-password") == true ->
                "Password is too weak. Use at least 6 characters"
            
            exception.message?.contains("USER_NOT_FOUND") == true ||
            exception.message?.contains("user-not-found") == true ->
                "No account found with this email"
            
            exception.message?.contains("USER_DISABLED") == true ||
            exception.message?.contains("user-disabled") == true ->
                "This account has been disabled"
            
            exception.message?.contains("TOO_MANY_REQUESTS") == true ||
            exception.message?.contains("too-many-requests") == true ->
                "Too many failed attempts. Please try again later"
            
            exception.message?.contains("NETWORK") == true ||
            exception.message?.contains("network") == true ->
                "Network error. Please check your connection"
            
            exception.message?.contains("INVALID_EMAIL") == true ||
            exception.message?.contains("invalid-email") == true ->
                "Please enter a valid email address"
            
            else -> exception.message ?: "An unexpected error occurred"
        }
        return Exception(message)
    }
}

