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
 * Event Board and History are specced but not yet backed by real endpoints
 * beyond GET /api/leaderboard - wire these up to
 * GET /api/events/active and a future GET /api/sessions endpoint next.
 */
@Composable
fun EventBoardScreen() = PlaceholderScreen("Turf War events land here once a session backlog exists to rank.")

@Composable
fun HistoryScreen() = PlaceholderScreen("Your completed walks will show up here after your first synced session.")

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
