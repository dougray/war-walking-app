package com.warwalking.app.ui

import com.warwalking.app.network.UserRegisterResponse

sealed class RegisterUiState {
    data object Idle : RegisterUiState()
    data object Loading : RegisterUiState()
    data class Success(val user: UserRegisterResponse) : RegisterUiState()
    data class Error(val errorMessage: String) : RegisterUiState()
}
