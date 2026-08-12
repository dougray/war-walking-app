package com.warwalking.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warwalking.app.data.AppDatabase
import com.warwalking.app.data.WalkSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Reads straight from the on-device Room database - no network involved. */
class HistoryViewModel(context: Context) : ViewModel() {
    private val dao = AppDatabase.get(context).walkSessionDao()

    val sessions: StateFlow<List<WalkSessionEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun renameSession(session: WalkSessionEntity, newTitle: String?) {
        viewModelScope.launch {
            dao.update(session.copy(title = newTitle))
        }
    }
}
