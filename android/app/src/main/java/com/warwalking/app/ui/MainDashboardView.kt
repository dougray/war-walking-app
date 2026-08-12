package com.warwalking.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warwalking.app.service.LiveScanEntry
import com.warwalking.app.service.LiveScanState

@Composable
fun MainDashboardView(
    currentStreak: Int,
    isWalking: Boolean,
    liveSteps: Int,
    liveAPs: Int,
    onStartWalk: () -> Unit,
    onStopWalk: () -> Unit
) {
    val scanEntries by LiveScanState.entries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StreakBanner(currentStreak)
        Spacer(modifier = Modifier.height(12.dp))
        StartStopButton(isWalking, onStartWalk, onStopWalk)
        Spacer(modifier = Modifier.height(12.dp))

        LiveScanPanel(
            entries = scanEntries,
            isWalking = isWalking,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        MetricsRow(liveSteps, liveAPs)

        AnimatedVisibility(
            visible = isWalking,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "Current Est. Points: ${liveSteps * liveAPs}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StreakBanner(currentStreak: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("🔥", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Current Streak", style = MaterialTheme.typography.labelMedium)
                Text(
                    "$currentStreak Days Active",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StartStopButton(isWalking: Boolean, onStartWalk: () -> Unit, onStopWalk: () -> Unit) {
    Button(
        onClick = { if (isWalking) onStopWalk() else onStartWalk() },
        shape = RoundedCornerShape(12.dp),
        // Start is the bold red "go" action; Stop reads as "currently active,
        // tap to end" - a dark grey fill with a red outline/text, rather than
        // a second loud red block, so the two states don't fight each other.
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isWalking) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
            contentColor = if (isWalking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
        ),
        border = if (isWalking) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().height(72.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = if (isWalking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isWalking) "Stop walking session" else "Start walking session",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isWalking) "STOP RUN" else "START WALK",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun LiveScanPanel(entries: List<LiveScanEntry>, isWalking: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                if (isWalking) "LIVE SCAN" else "LAST SCAN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (isWalking) "Scanning..." else "Start a walk to see nearby networks appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.mac }) { entry -> ScanEntryRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun ScanEntryRow(entry: LiveScanEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.ssid.ifBlank { "(hidden)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                entry.mac,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            entry.type,
            style = MaterialTheme.typography.labelSmall,
            color = if (entry.type == "WIFI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${entry.rssi}dBm",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricsRow(liveSteps: Int, liveAPs: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Card(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MetricLabel("👟 Live Steps")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$liveSteps",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MetricLabel("📡 APs Discovered")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$liveAPs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// bodyMedium's default size (monospace, so wider per character than a sans
// font) was enough for "APs Discovered" to wrap to two lines in its
// half-width card while "Live Steps" stayed on one - misaligning the two
// numbers below them. A smaller single-line label fixes it the same way the
// bottom nav's labels were fixed.
@Composable
private fun MetricLabel(text: String) {
    Text(text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
