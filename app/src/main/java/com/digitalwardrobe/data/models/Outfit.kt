package com.digitalwardrobe.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Outfit data model for Firestore.
 * Represents a saved combination of garments.
 */
data class Outfit(
    @DocumentId
    val id: String = "",

    val userId: String = "",

    val name: String = "",

    // List of garment IDs that make up this outfit
    val garmentIds: List<String> = emptyList(),

    // Optional rendered outfit image URL (generated after AI processing)
    val imageUrl: String? = null,

    // Optional description
    val description: String? = null,

    // Tags for categorization
    val tags: List<String> = emptyList(),

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null
)
