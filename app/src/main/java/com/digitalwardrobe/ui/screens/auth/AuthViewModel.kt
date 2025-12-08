package com.digitalwardrobe.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwardrobe.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens (Login, SignUp, ForgotPassword).
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthNavigationEvent>()
    val authEvent = _authEvent.asSharedFlow()

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    init {
        // Observe auth state changes
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
                if (user != null) {
                    _authEvent.emit(AuthNavigationEvent.NavigateToHome)
                }
            }
        }
    }

    /**
     * Handle login with email and password.
     */
    fun login(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            authRepository.signIn(email.trim(), password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    _authEvent.emit(AuthNavigationEvent.NavigateToHome)
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = exception.message
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Handle user registration.
     */
    fun signUp(email: String, password: String, displayName: String) {
        if (!validateSignUpInput(email, password, displayName)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            authRepository.signUp(
                email = email.trim(),
                password = password,
                displayName = displayName.trim()
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    _authEvent.emit(AuthNavigationEvent.NavigateToHome)
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = exception.message
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Send password reset email.
     */
    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            authRepository.sendPasswordResetEmail(email.trim()).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _authEvent.emit(AuthNavigationEvent.ShowPasswordResetSent)
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = exception.message
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Sign out the current user.
     */
    fun logout() {
        authRepository.signOut()
        _uiState.update { AuthUiState() }
        viewModelScope.launch {
            _authEvent.emit(AuthNavigationEvent.NavigateToLogin)
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please enter your email") }
                false
            }
            !email.contains("@") -> {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email") }
                false
            }
            password.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please enter your password") }
                false
            }
            else -> true
        }
    }

    private fun validateSignUpInput(
        email: String, 
        password: String, 
        displayName: String
    ): Boolean {
        return when {
            displayName.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please enter your name") }
                false
            }
            email.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please enter your email") }
                false
            }
            !email.contains("@") || !email.contains(".") -> {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email") }
                false
            }
            password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
                false
            }
            else -> true
        }
    }
}

/**
 * Navigation events for authentication flow.
 */
sealed class AuthNavigationEvent {
    data object NavigateToHome : AuthNavigationEvent()
    data object NavigateToLogin : AuthNavigationEvent()
    data object NavigateToSignUp : AuthNavigationEvent()
    data object ShowPasswordResetSent : AuthNavigationEvent()
}

