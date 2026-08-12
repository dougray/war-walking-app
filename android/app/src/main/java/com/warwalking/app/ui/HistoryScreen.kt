package com.warwalking.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warwalking.app.data.WalkSessionEntity
import com.warwalking.app.data.decodeRoutePoints
import com.warwalking.app.data.pointsEarned
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min

private val displayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    var editing by remember { mutableStateOf<WalkSessionEntity?>(null) }

    if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Your completed walks will show up here after your first walk.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                HistorySessionCard(session, onClick = { editing = session })
            }
        }
    }

    editing?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { editing = null },
            onSave = { title ->
                viewModel.renameSession(session, title)
                editing = null
            }
        )
    }
}

@Composable
private fun HistorySessionCard(session: WalkSessionEntity, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                session.title?.takeIf { it.isNotBlank() } ?: "Untitled walk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(session.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            RouteSketch(session.decodeRoutePoints())
            Spacer(Modifier.height(8.dp))
            Text(
                "${session.stepsCounted} steps · ${session.apDiscovered} APs · ${session.pointsEarned} pts",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (session.wigleTransId != null) "Synced to WiGLE" else "Not yet synced to WiGLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// A relative-shape sketch, not a real map - no streets, no scale, no tiles,
// no network call or API key (see the "keep this free" note in the repo).
// Just the walk's own points connected by lines, scaled to fit the box while
// preserving aspect ratio so a straight walk doesn't get stretched into a
// diagonal fill. Longitude degrees cover less real distance than latitude
// degrees away from the equator, corrected by cos(latitude) so the sketch's
// proportions roughly match how the walk actually looked.
@Composable
private fun RouteSketch(points: List<Pair<Double, Double>>) {
    if (points.size < 2) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text(
                "No route recorded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val routeColor = MaterialTheme.colorScheme.primary
    val endColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val lats = points.map { it.first }
        val lons = points.map { it.second }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLon = lons.min()
        val maxLon = lons.max()
        val latSpan = (maxLat - minLat).coerceAtLeast(0.00001)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.00001)

        val padding = 12f
        val availW = size.width - padding * 2
        val availH = size.height - padding * 2

        val lonCorrection = cos(Math.toRadians((minLat + maxLat) / 2.0)).coerceAtLeast(0.1)
        val correctedLonSpan = (lonSpan * lonCorrection).toFloat()
        val scale = min(availW / correctedLonSpan, availH / latSpan.toFloat())

        fun project(lat: Double, lon: Double): Offset {
            val x = padding + ((lon - minLon) * lonCorrection).toFloat() * scale
            val y = padding + (maxLat - lat).toFloat() * scale // screen y grows down; north should be up
            return Offset(x, y)
        }

        val path = Path()
        points.forEachIndexed { index, (lat, lon) ->
            val offset = project(lat, lon)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(path, color = routeColor, style = Stroke(width = 4f))

        val start = project(points.first().first, points.first().second)
        val end = project(points.last().first, points.last().second)
        drawCircle(color = endColor, radius = 6f, center = start)
        drawCircle(color = routeColor, radius = 6f, center = end)
    }
}

@Composable
private fun EditSessionDialog(session: WalkSessionEntity, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var title by remember { mutableStateOf(session.title ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename walk") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim().ifBlank { null }) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(displayTimeFormat)
