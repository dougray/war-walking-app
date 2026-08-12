package com.warwalking.app.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warwalking.app.UserSession
import com.warwalking.app.network.SessionSummary
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val displayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = remember { HistoryViewModel() }) {
    val context = LocalContext.current
    val userId = remember { UserSession.getUserId(context) }
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<SessionSummary?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) viewModel.loadSessions(userId)
    }

    when {
        userId == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Register in Settings to start building your walk history.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> when (val current = state) {
            is HistoryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is HistoryUiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
            }
            is HistoryUiState.Success -> {
                if (current.sessions.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Your completed walks will show up here after your first synced session.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(current.sessions, key = { it.sessionId }) { session ->
                            HistorySessionCard(session, onClick = { editing = session })
                        }
                    }
                }
            }
        }
    }

    val session = editing
    if (session != null && userId != null) {
        EditSessionDialog(
            session = session,
            onDismiss = { editing = null },
            onSave = { title, isPublic ->
                viewModel.updateSession(userId, session.sessionId, title, isPublic)
                editing = null
            }
        )
    }
}

@Composable
private fun HistorySessionCard(session: SessionSummary, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    session.title?.takeIf { it.isNotBlank() } ?: "Untitled walk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (session.isPublic) "Public" else "Private",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(session.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${session.stepsCounted} steps · ${session.apDiscovered} APs · ${session.pointsEarned} pts",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (session.wigleFileId != null) "Synced to WiGLE" else "Not yet synced to WiGLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditSessionDialog(session: SessionSummary, onDismiss: () -> Unit, onSave: (String?, Boolean) -> Unit) {
    var title by remember { mutableStateOf(session.title ?: "") }
    var isPublic by remember { mutableStateOf(session.isPublic) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit walk") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Show in public feed")
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, isPublic) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTimestamp(iso: String): String = try {
    OffsetDateTime.parse(iso).format(displayTimeFormat)
} catch (e: Exception) {
    iso
}
