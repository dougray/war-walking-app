package com.warwalking.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.network.SessionSummary
import com.warwalking.app.network.WarWalkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(val sessions: List<SessionSummary>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel(
    private val repository: WarWalkingRepository = WarWalkingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadSessions(userId: Int) {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            repository.getUserSessions(userId)
                .onSuccess { sessions -> _uiState.value = HistoryUiState.Success(sessions) }
                .onFailure { e -> _uiState.value = HistoryUiState.Error(e.localizedMessage ?: "Couldn't load your history.") }
        }
    }

    fun updateSession(userId: Int, sessionId: Int, title: String?, isPublic: Boolean) {
        viewModelScope.launch {
            repository.updateSession(sessionId, userId, title, isPublic).onSuccess {
                loadSessions(userId)
            }
        }
    }
}
