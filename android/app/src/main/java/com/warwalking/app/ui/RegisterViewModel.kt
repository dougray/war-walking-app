package com.warwalking.app.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.network.WarWalkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: WarWalkingRepository = WarWalkingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun performResearcherRegistration(username: String, email: String, apiName: String, apiToken: String) {
        if (username.isBlank() || email.isBlank() || apiName.isBlank() || apiToken.isBlank()) {
            _uiState.value = RegisterUiState.Error("All registration fields are required.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = RegisterUiState.Error("Please provide a valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = repository.register(username.trim(), email.trim(), apiName.trim(), apiToken.trim())
            result.onSuccess { response ->
                _uiState.value = RegisterUiState.Success(response)
            }.onFailure { exception ->
                _uiState.value = RegisterUiState.Error(
                    exception.localizedMessage ?: "Unknown connection failure to backend server."
                )
            }
        }
    }

    fun clearErrorState() {
        if (_uiState.value is RegisterUiState.Error) {
            _uiState.value = RegisterUiState.Idle
        }
    }
}
