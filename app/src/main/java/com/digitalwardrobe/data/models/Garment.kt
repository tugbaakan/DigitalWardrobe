package com.digitalwardrobe.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Garment data model for Firestore.
 */
data class Garment(
    @DocumentId
    val id: String = "",

    val userId: String = "",

    // Image stored in Firebase Storage
    val imageUrl: String = "",

    // Metadata tags
    val type: GarmentType = GarmentType.SHIRT,
    val color: GarmentColor = GarmentColor.BLACK,
    val formality: GarmentFormality = GarmentFormality.CASUAL,
    val fit: GarmentFit = GarmentFit.REGULAR,

    // Optional description
    val description: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null
)

/**
 * Garment type enumeration.
 */
enum class GarmentType {
    SHIRT,
    TROUSERS,
    SKIRT,
    JACKET,
    DRESS,
    SHOES,
    ACCESSORY,
    OTHER
}

/**
 * Garment color enumeration.
 */
enum class GarmentColor {
    BLACK,
    WHITE,
    NAVY,
    RED,
    BLUE,
    GREEN,
    YELLOW,
    PURPLE,
    PINK,
    GRAY,
    BROWN,
    BEIGE,
    PATTERNED,
    OTHER
}

/**
 * Garment formality enumeration.
 */
enum class GarmentFormality {
    CASUAL,
    BUSINESS,
    FORMAL,
    ATHLETIC,
    EVENING
}

/**
 * Garment fit/cut enumeration.
 */
enum class GarmentFit {
    SLIM,
    REGULAR,
    OVERSIZED,
    LOOSE
}
