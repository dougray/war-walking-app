package com.warwalking.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LiveScanEntry(
    val mac: String,
    val ssid: String,
    val rssi: Int,
    val type: String, // "WIFI" or "BLE"
    val lastSeenAt: Long
)

/**
 * In-process, in-memory only - deliberately not Room or a broadcast Intent.
 * The service and the dashboard composable run in the same process, so a
 * plain shared StateFlow is the simplest thing that works; nothing here
 * needs to survive process death or cross an IPC boundary. Most-recently-
 * seen network floats to the top, like a live radar.
 */
object LiveScanState {
    private const val MAX_ENTRIES = 100

    private val _entries = MutableStateFlow<List<LiveScanEntry>>(emptyList())
    val entries: StateFlow<List<LiveScanEntry>> = _entries.asStateFlow()

    fun addOrUpdate(entry: LiveScanEntry) {
        _entries.update { current ->
            (listOf(entry) + current.filterNot { it.mac == entry.mac }).take(MAX_ENTRIES)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
