package com.warwalking.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.WigleCredentialStore
import com.warwalking.app.network.WigleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Verifies a WiGLE API Name/Token pair against the live API, then saves it
 *  on-device. There's no separate WarWalker account to register for anymore -
 *  the WiGLE account itself is the identity. */
class CredentialViewModel(
    private val context: Context,
    private val repository: WigleRepository = WigleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CredentialUiState>(CredentialUiState.Idle)
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    fun verifyAndSave(apiName: String, apiToken: String) {
        if (apiName.isBlank() || apiToken.isBlank()) {
            _uiState.value = CredentialUiState.Error("Both the API Name and API Token are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CredentialUiState.Loading
            val result = repository.verifyCredentials(apiName.trim(), apiToken.trim())
            result.onSuccess { profile ->
                WigleCredentialStore.save(context, apiName.trim(), apiToken.trim(), profile.userid)
                _uiState.value = CredentialUiState.Success(profile)
            }.onFailure { exception ->
                _uiState.value = CredentialUiState.Error(
                    exception.localizedMessage ?: "Unable to reach WiGLE's servers."
                )
            }
        }
    }

    fun signOut() {
        WigleCredentialStore.clear(context)
        _uiState.value = CredentialUiState.Idle
    }
}
