package com.warwalking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Event Board (Turf War leaderboards) is specced but not built yet - deferred
 * in favor of the social feed (FeedScreen.kt) and history (HistoryScreen.kt),
 * which now have real endpoints behind them. GET /api/events/active already
 * exists on the backend for whenever this gets picked up.
 */
@Composable
fun EventBoardScreen() = PlaceholderScreen("Turf War events land here once a session backlog exists to rank.")

@Composable
private fun PlaceholderScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
