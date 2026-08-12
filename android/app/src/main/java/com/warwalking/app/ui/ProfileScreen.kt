package com.warwalking.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warwalking.app.data.WalkSessionEntity
import com.warwalking.app.data.pointsEarned
import com.warwalking.app.network.WigleUserStandings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsState()
    val localHistory by viewModel.localHistory.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when (val current = state) {
            is ProfileUiState.NotSignedIn -> InfoCard(
                "Add your WiGLE API keys in Settings to see your live rank and totals here."
            )
            is ProfileUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            is ProfileUiState.Error -> InfoCard(current.message, isError = true)
            is ProfileUiState.Success -> WigleStatsCard(current.stats.statistics)
        }

        Spacer(Modifier.height(16.dp))
        LocalHistoryCard(localHistory)
    }
}

@Composable
private fun InfoCard(message: String, isError: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WigleStatsCard(stats: WigleUserStandings?) {
    if (stats == null) {
        InfoCard("WiGLE didn't return any statistics for this account yet.")
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("WiGLE Standing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Global Rank", "#${stats.rank}")
                StatColumn("Month Rank", "#${stats.monthRank}")
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("WiFi Found", "${stats.discoveredWiFi}")
                StatColumn("Cell Found", "${stats.discoveredCell}")
                StatColumn("BT Found", "${stats.discoveredBt}")
            }
            Spacer(Modifier.height(16.dp))

            val delta = stats.eventMonthCount - stats.eventPrevMonthCount
            val trend = if (delta >= 0) "▲" else "▼"
            Text(
                "This month: ${stats.eventMonthCount} (last month: ${stats.eventPrevMonthCount}) $trend",
                style = MaterialTheme.typography.bodyMedium,
                color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LocalHistoryCard(sessions: List<WalkSessionEntity>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Last 7 Days (local)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Points per day, tracked entirely on this device - WiGLE has no concept of steps or session scoring.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val pointsByDay = sessions
                .groupBy { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
                .mapValues { (_, daySessions) -> daySessions.sumOf { it.pointsEarned } }
            val bars = days.map { day -> day.format(DateTimeFormatter.ofPattern("EEE")) to (pointsByDay[day] ?: 0L) }

            BarChart(bars, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Long>>, barColor: Color, modifier: Modifier = Modifier) {
    val maxValue = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val slotWidth = size.width / bars.size
            val barWidth = slotWidth * 0.5f
            bars.forEachIndexed { index, (_, value) ->
                val barHeight = size.height * (value.toFloat() / maxValue.toFloat())
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            bars.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
