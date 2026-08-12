package com.warwalking.app.ui

import com.warwalking.app.network.WigleProfileResponse

sealed class CredentialUiState {
    data object Idle : CredentialUiState()
    data object Loading : CredentialUiState()
    data class Success(val profile: WigleProfileResponse) : CredentialUiState()
    data class Error(val errorMessage: String) : CredentialUiState()
}
