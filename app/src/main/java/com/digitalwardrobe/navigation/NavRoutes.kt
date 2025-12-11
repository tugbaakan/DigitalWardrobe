package com.digitalwardrobe.navigation

/**
 * Navigation routes for the app.
 */
sealed class NavRoutes(val route: String) {
    // Auth routes
    data object Login : NavRoutes("login")
    data object SignUp : NavRoutes("signup")
    
    // Main routes
    data object Home : NavRoutes("home")
    data object Profile : NavRoutes("profile")
    
    // Wardrobe routes (Phase 2)
    data object BodyPhotoUpload : NavRoutes("body_photo_upload")
    data object Wardrobe : NavRoutes("wardrobe")
    data object AddGarment : NavRoutes("add_garment")
    data object WardrobeView : NavRoutes("wardrobe_view")
    data object GarmentMetadata : NavRoutes("garment_metadata/{imageUri}") {
        fun createRoute(imageUri: String) = "garment_metadata/$imageUri"
    }
    data object GarmentDetail : NavRoutes("garment/{garmentId}") {
        fun createRoute(garmentId: String) = "garment/$garmentId"
    }
    
    // Outfit routes (Phase 4)
    data object Outfits : NavRoutes("outfits")
    data object OutfitDetail : NavRoutes("outfit/{outfitId}") {
        fun createRoute(outfitId: String) = "outfit/$outfitId"
    }
    data object TryOn : NavRoutes("try_on")
}

/**
 * Navigation argument keys.
 */
object NavArgs {
    const val GARMENT_ID = "garmentId"
    const val OUTFIT_ID = "outfitId"
}
