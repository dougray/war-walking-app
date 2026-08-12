package com.warwalking.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.network.FeedItem
import com.warwalking.app.network.WarWalkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data class Success(val items: List<FeedItem>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel(
    private val repository: WarWalkingRepository = WarWalkingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    fun loadFeed(viewerUserId: Int?) {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            repository.getFeed(viewerUserId)
                .onSuccess { items -> _uiState.value = FeedUiState.Success(items) }
                .onFailure { e -> _uiState.value = FeedUiState.Error(e.localizedMessage ?: "Couldn't load the feed.") }
        }
    }

    fun toggleKudos(item: FeedItem, viewerUserId: Int) {
        val current = _uiState.value as? FeedUiState.Success ?: return

        // Optimistic flip so the tap feels instant; the real kudos_count from
        // the response corrects it, and a failure rolls the item back.
        _uiState.value = FeedUiState.Success(current.items.map {
            if (it.sessionId == item.sessionId) {
                it.copy(
                    viewerHasKudos = !it.viewerHasKudos,
                    kudosCount = it.kudosCount + if (it.viewerHasKudos) -1 else 1
                )
            } else it
        })

        viewModelScope.launch {
            val result = if (item.viewerHasKudos) {
                repository.removeKudos(item.sessionId, viewerUserId)
            } else {
                repository.giveKudos(item.sessionId, viewerUserId)
            }

            val latest = _uiState.value as? FeedUiState.Success ?: return@launch
            result.onSuccess { response ->
                _uiState.value = FeedUiState.Success(latest.items.map {
                    if (it.sessionId == item.sessionId) it.copy(kudosCount = response.kudosCount) else it
                })
            }.onFailure {
                _uiState.value = FeedUiState.Success(latest.items.map {
                    if (it.sessionId == item.sessionId) item else it
                })
            }
        }
    }
}
