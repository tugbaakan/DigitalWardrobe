package com.digitalwardrobe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.digitalwardrobe.ui.screens.auth.AuthNavigationEvent
import com.digitalwardrobe.ui.screens.auth.AuthViewModel
import com.digitalwardrobe.ui.screens.auth.ForgotPasswordDialog
import com.digitalwardrobe.ui.screens.auth.LoginScreen
import com.digitalwardrobe.ui.screens.auth.PasswordResetSentDialog
import com.digitalwardrobe.ui.screens.auth.SignUpScreen
import com.digitalwardrobe.ui.screens.bodyphoto.BodyPhotoUploadScreen
import com.digitalwardrobe.ui.screens.garment.AddGarmentScreen
import com.digitalwardrobe.ui.screens.garment.GarmentDetailScreen
import com.digitalwardrobe.ui.screens.garment.GarmentMetadata
import com.digitalwardrobe.ui.screens.garment.GarmentMetadataScreen
import com.digitalwardrobe.ui.screens.home.HomeScreen
import com.digitalwardrobe.ui.screens.wardrobe.WardrobeScreen
import com.digitalwardrobe.ui.screens.profile.ProfileNavigationEvent
import com.digitalwardrobe.ui.screens.profile.ProfileScreen
import com.digitalwardrobe.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Main navigation host for the app.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    startDestination: String = if (authViewModel.isLoggedIn) NavRoutes.Home.route else NavRoutes.Login.route
) {
    // Observe auth navigation events
    LaunchedEffect(Unit) {
        authViewModel.authEvent.collectLatest { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToLogin -> {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToSignUp -> {
                    navController.navigate(NavRoutes.SignUp.route)
                }
                is AuthNavigationEvent.ShowPasswordResetSent -> {
                    // Handled within the screen
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login Screen
        composable(NavRoutes.Login.route) {
            val uiState by authViewModel.uiState.collectAsState()
            var showForgotPasswordDialog by remember { mutableStateOf(false) }
            var showPasswordResetSentDialog by remember { mutableStateOf(false) }

            // Listen for password reset sent event
            LaunchedEffect(Unit) {
                authViewModel.authEvent.collectLatest { event ->
                    if (event is AuthNavigationEvent.ShowPasswordResetSent) {
                        showForgotPasswordDialog = false
                        showPasswordResetSentDialog = true
                    }
                }
            }

            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onSignUpClick = {
                    navController.navigate(NavRoutes.SignUp.route)
                },
                onForgotPasswordClick = {
                    showForgotPasswordDialog = true
                },
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage
            )

            // Forgot Password Dialog
            if (showForgotPasswordDialog) {
                ForgotPasswordDialog(
                    onDismiss = { showForgotPasswordDialog = false },
                    onSendResetEmail = { email ->
                        authViewModel.forgotPassword(email)
                    },
                    isLoading = uiState.isLoading
                )
            }

            // Password Reset Sent Dialog
            if (showPasswordResetSentDialog) {
                PasswordResetSentDialog(
                    onDismiss = { showPasswordResetSentDialog = false }
                )
            }
        }

        // Sign Up Screen
        composable(NavRoutes.SignUp.route) {
            val uiState by authViewModel.uiState.collectAsState()

            SignUpScreen(
                onSignUpClick = { email, password, displayName ->
                    authViewModel.signUp(email, password, displayName)
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                },
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage
            )
        }

        // Home Screen
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onProfileClick = {
                    navController.navigate(NavRoutes.Profile.route)
                },
                onWardrobeClick = {
                    navController.navigate(NavRoutes.WardrobeView.route)
                },
                onOutfitsClick = {
                    // Will navigate to outfits in Phase 4
                    // navController.navigate(NavRoutes.Outfits.route)
                },
                onTryOnClick = {
                    // Will navigate to try-on in Phase 4
                    // navController.navigate(NavRoutes.TryOn.route)
                }
            )
        }

        // Profile Screen
        composable(NavRoutes.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            val uiState by profileViewModel.uiState.collectAsState()

            // Observe navigation events
            LaunchedEffect(Unit) {
                profileViewModel.navigationEvent.collectLatest { event ->
                    when (event) {
                        is ProfileNavigationEvent.NavigateToLogin -> {
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }

            ProfileScreen(
                displayName = uiState.displayName,
                email = uiState.email,
                onBackClick = {
                    navController.popBackStack()
                },
                onLogoutClick = {
                    profileViewModel.logout()
                },
                onUpdateDisplayName = { newName ->
                    profileViewModel.updateDisplayName(newName)
                },
                isLoading = uiState.isLoading,
                successMessage = uiState.successMessage,
                errorMessage = uiState.errorMessage,
                onClearMessage = {
                    profileViewModel.clearMessage()
                }
            )
        }

        // Body Photo Upload Screen (Phase 2)
        composable(NavRoutes.BodyPhotoUpload.route) {
            BodyPhotoUploadScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPhotoSelected = { photoUri ->
                    // TODO: Handle photo selection - will save to storage and navigate next
                    // For now, just show a placeholder
                }
            )
        }

        // Add Garment Screen (Phase 2)
        composable(NavRoutes.AddGarment.route) {
            AddGarmentScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onGarmentAdded = { garmentUri ->
                    // Navigate to garment metadata tagging screen
                    navController.navigate(NavRoutes.GarmentMetadata.createRoute(garmentUri))
                }
            )
        }

        // Garment Metadata Screen (Phase 2)
        composable(NavRoutes.GarmentMetadata.route) { backStackEntry ->
            val encodedImageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = java.net.URLDecoder.decode(encodedImageUri, "UTF-8")
            GarmentMetadataScreen(
                garmentImageUri = imageUri,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveGarment = { metadata ->
                    // TODO: Save garment to Firebase Storage and Firestore
                    // For now, just navigate back to home
                    navController.popBackStack()
                }
            )
        }

        // Wardrobe View Screen (Phase 2)
        composable(NavRoutes.WardrobeView.route) {
            WardrobeScreen(
                onAddGarmentClick = {
                    navController.navigate(NavRoutes.AddGarment.route)
                },
                onGarmentClick = { garmentId ->
                    navController.navigate(NavRoutes.GarmentDetail.createRoute(garmentId))
                }
            )
        }

        // Garment Detail Screen (Phase 2)
        composable(NavRoutes.GarmentDetail.route) { backStackEntry ->
            val garmentId = backStackEntry.arguments?.getString("garmentId") ?: ""
            GarmentDetailScreen(
                garmentId = garmentId,
                onBackClick = {
                    navController.popBackStack()
                },
                onGarmentDeleted = {
                    // Navigate back to wardrobe after deletion
                    navController.popBackStack()
                }
            )
        }

        // Placeholder for future screens (Phase 2, 4)
        // composable(NavRoutes.Wardrobe.route) { WardrobeScreen(...) }
        // composable(NavRoutes.Outfits.route) { OutfitsScreen(...) }
        // composable(NavRoutes.TryOn.route) { TryOnScreen(...) }
    }
}
