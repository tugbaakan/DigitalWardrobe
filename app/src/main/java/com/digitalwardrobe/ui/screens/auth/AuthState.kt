package com.digitalwardrobe.ui.screens.auth

/**
 * Represents the UI state for authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = ""
)

/**
 * Sealed class representing authentication events.
 */
sealed class AuthEvent {
    data class Login(val email: String, val password: String) : AuthEvent()
    data class SignUp(val email: String, val password: String, val displayName: String) : AuthEvent()
    data class ForgotPassword(val email: String) : AuthEvent()
    data object Logout : AuthEvent()
    data object ClearError : AuthEvent()
}

/**
 * Sealed class representing authentication results.
 */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult()
}

