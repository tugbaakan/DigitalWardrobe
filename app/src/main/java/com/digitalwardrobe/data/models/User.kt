package com.digitalwardrobe.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * User profile data stored in Firestore.
 * Note: Authentication is handled by Firebase Auth, this stores additional profile info.
 */
data class User(
    @DocumentId
    val id: String = "",

    val email: String = "",
    val displayName: String = "",

    // Optional additional profile fields
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null
)
