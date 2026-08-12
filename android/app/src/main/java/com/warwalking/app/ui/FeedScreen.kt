package com.warwalking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warwalking.app.UserSession
import com.warwalking.app.network.FeedItem
import com.warwalking.app.network.SessionComment
import com.warwalking.app.network.WarWalkingRepository
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val displayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
fun FeedScreen(viewModel: FeedViewModel = remember { FeedViewModel() }) {
    val context = LocalContext.current
    val viewerUserId = remember { UserSession.getUserId(context) }
    val state by viewModel.uiState.collectAsState()
    var commentsSessionId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.loadFeed(viewerUserId) }

    when (val current = state) {
        is FeedUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is FeedUiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(current.message, color = MaterialTheme.colorScheme.error)
        }
        is FeedUiState.Success -> {
            if (current.items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No public walks yet - share one from History after your next walk.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(current.items, key = { it.sessionId }) { item ->
                        FeedItemCard(
                            item = item,
                            onKudosClick = { viewerUserId?.let { viewModel.toggleKudos(item, it) } },
                            onCommentsClick = { commentsSessionId = item.sessionId }
                        )
                    }
                }
            }
        }
    }

    commentsSessionId?.let { sessionId ->
        CommentsDialog(sessionId = sessionId, viewerUserId = viewerUserId, onDismiss = { commentsSessionId = null })
    }
}

@Composable
private fun FeedItemCard(item: FeedItem, onKudosClick: () -> Unit, onCommentsClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.username,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTimestamp(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.title?.takeIf { it.isNotBlank() } ?: "Untitled walk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${item.stepsCounted} steps · ${item.apDiscovered} APs · ${item.pointsEarned} pts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onKudosClick) {
                    Icon(
                        imageVector = if (item.viewerHasKudos) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (item.viewerHasKudos) "Remove kudos" else "Give kudos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text("${item.kudosCount}", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = onCommentsClick) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = "View comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${item.commentCount}", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CommentsDialog(sessionId: Int, viewerUserId: Int?, onDismiss: () -> Unit) {
    val repository = remember { WarWalkingRepository() }
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<SessionComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newComment by remember { mutableStateOf("") }

    suspend fun refresh() {
        repository.getComments(sessionId).onSuccess { comments = it }
        loading = false
    }

    LaunchedEffect(sessionId) { refresh() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Comments") },
        text = {
            Column {
                when {
                    loading -> CircularProgressIndicator()
                    comments.isEmpty() -> Text(
                        "No comments yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                        comments.forEach { comment ->
                            Text(
                                buildString {
                                    append(comment.username)
                                    append(": ")
                                    append(comment.body)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                if (viewerUserId != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            label = { Text("Add a comment") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val body = newComment.trim()
                            if (body.isNotEmpty()) {
                                scope.launch {
                                    repository.addComment(sessionId, viewerUserId, body)
                                        .onSuccess {
                                            newComment = ""
                                            refresh()
                                        }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Send, contentDescription = "Post comment")
                        }
                    }
                }
            }
        }
    )
}

private fun formatTimestamp(iso: String): String = try {
    OffsetDateTime.parse(iso).format(displayTimeFormat)
} catch (e: Exception) {
    iso
}
