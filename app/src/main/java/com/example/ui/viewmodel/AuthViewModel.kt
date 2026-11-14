package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSeedReady: Boolean = false
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository

    val currentUserId: StateFlow<String?> = repo.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDarkMode: StateFlow<Boolean> = repo.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasSeenOnboarding: StateFlow<Boolean> = repo.hasSeenOnboardingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.initializeSeedData()
            _uiState.value = _uiState.value.copy(isSeedReady = true)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { repo.setOnboardingSeen() }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { repo.setDarkMode(enabled) }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repo.login(identifier, password)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun loginDemoAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            container.initializeSeedData()
            val result = repo.loginDemoAccount()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        avatarUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repo.register(fullName, username, email, password, confirmPassword, avatarUri)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun resetPassword(email: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repo.resetPassword(email, newPassword, confirmPassword)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Password updated! You can sign in now."
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }
}
