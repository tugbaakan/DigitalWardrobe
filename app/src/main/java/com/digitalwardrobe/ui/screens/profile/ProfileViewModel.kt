package com.digitalwardrobe.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwardrobe.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadUserProfile()
    }

    /**
     * Load current user profile data.
     */
    private fun loadUserProfile() {
        authRepository.currentUser?.let { user ->
            _uiState.update {
                it.copy(
                    displayName = user.displayName ?: "",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString()
                )
            }
        }
    }

    /**
     * Update user's display name.
     */
    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            authRepository.updateDisplayName(newName.trim()).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = newName.trim(),
                            successMessage = "Name updated successfully"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Failed to update name"
                        )
                    }
                }
            )
        }
    }

    /**
     * Log out the current user.
     */
    fun logout() {
        authRepository.signOut()
        viewModelScope.launch {
            _navigationEvent.emit(ProfileNavigationEvent.NavigateToLogin)
        }
    }

    /**
     * Clear success/error messages.
     */
    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    /**
     * Refresh user profile data.
     */
    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            authRepository.reloadUser().fold(
                onSuccess = {
                    loadUserProfile()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }
}

/**
 * UI state for the Profile screen.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * Navigation events for the Profile screen.
 */
sealed class ProfileNavigationEvent {
    data object NavigateToLogin : ProfileNavigationEvent()
}

