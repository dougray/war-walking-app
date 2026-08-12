package com.warwalking.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.WigleCredentialStore
import com.warwalking.app.data.AppDatabase
import com.warwalking.app.data.WalkSessionEntity
import com.warwalking.app.network.WigleRepository
import com.warwalking.app.network.WigleUserStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object NotSignedIn : ProfileUiState()
    data object Loading : ProfileUiState()
    data class Success(val stats: WigleUserStatsResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val context: Context,
    private val repository: WigleRepository = WigleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _localHistory = MutableStateFlow<List<WalkSessionEntity>>(emptyList())
    val localHistory: StateFlow<List<WalkSessionEntity>> = _localHistory.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _localHistory.value = AppDatabase.get(context).walkSessionDao().getAll()
        }

        val apiName = WigleCredentialStore.getApiName(context)
        val apiToken = WigleCredentialStore.getApiToken(context)
        if (apiName == null || apiToken == null) {
            _uiState.value = ProfileUiState.NotSignedIn
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repository.fetchUserStats(apiName, apiToken)
                .onSuccess { _uiState.value = ProfileUiState.Success(it) }
                .onFailure { _uiState.value = ProfileUiState.Error(it.localizedMessage ?: "Couldn't load your WiGLE stats.") }
        }
    }
}
