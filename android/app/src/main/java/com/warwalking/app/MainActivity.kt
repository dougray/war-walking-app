package com.warwalking.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import com.warwalking.app.data.AppDatabase
import com.warwalking.app.data.StreakCalculator
import com.warwalking.app.health.HealthConnectManager
import com.warwalking.app.service.WarWalkingService
import com.warwalking.app.ui.CredentialViewModel
import com.warwalking.app.ui.HistoryScreen
import com.warwalking.app.ui.HistoryViewModel
import com.warwalking.app.ui.MainDashboardView
import com.warwalking.app.ui.ProfileScreen
import com.warwalking.app.ui.ProfileViewModel
import com.warwalking.app.ui.ScanSettings
import com.warwalking.app.ui.SettingsScreen
import com.warwalking.app.ui.theme.WarWalkingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarWalkingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WarWalkingApp()
                }
            }
        }
    }
}

@Composable
private fun WarWalkingApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context.applicationContext) }
    val credentialViewModel = remember { CredentialViewModel(context.applicationContext) }
    val historyViewModel = remember { HistoryViewModel(context.applicationContext) }
    val profileViewModel = remember { ProfileViewModel(context.applicationContext) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isWalking by remember { mutableStateOf(false) }
    var liveSteps by remember { mutableIntStateOf(0) }
    var liveAPs by remember { mutableIntStateOf(0) }
    var scanSettings by remember { mutableStateOf(ScanSettings()) }

    // Recomputed from the full local session history on every change - see
    // StreakCalculator for why that's a correctness improvement over the
    // incremental version this replaced.
    val allSessions by remember { AppDatabase.get(context.applicationContext).walkSessionDao().observeAll() }
        .collectAsState(initial = emptyList())
    val currentStreak = remember(allSessions) { StreakCalculator.currentStreak(allSessions) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { /* granted set ignored here; checked again lazily via hasPermissions() before each session */ }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // startForeground(type=location) throws SecurityException if fired before this
        // grant lands, so the service is only ever started from here or from the
        // already-granted fast path below - never optimistically alongside the request.
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            liveSteps = 0
            liveAPs = 0
            isWalking = true
            val intent = Intent(context, WarWalkingService::class.java).setAction(WarWalkingService.ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        } else {
            Toast.makeText(context, "Location permission is required to start a walk.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WarWalkingService.ACTION_UI_UPDATE -> {
                        if (intent.hasExtra(WarWalkingService.EXTRA_AP_COUNT)) {
                            liveAPs = intent.getIntExtra(WarWalkingService.EXTRA_AP_COUNT, liveAPs)
                        }
                        if (intent.hasExtra(WarWalkingService.EXTRA_LIVE_STEPS)) {
                            liveSteps = intent.getIntExtra(WarWalkingService.EXTRA_LIVE_STEPS, liveSteps)
                        }
                    }
                    WarWalkingService.ACTION_SESSION_SYNC_RESULT -> {
                        val message = intent.getStringExtra(WarWalkingService.EXTRA_SYNC_MESSAGE)
                        if (message != null) Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(WarWalkingService.ACTION_UI_UPDATE)
            addAction(WarWalkingService.ACTION_SESSION_SYNC_RESULT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun startWalk() {
        // Health Connect and Bluetooth/notification prompts are best-effort and don't
        // gate the walk - only location does, since the service can't legally call
        // startForeground(type=location) without it.
        coroutineScope.launch {
            if (healthConnectManager.isAvailable && !healthConnectManager.hasPermissions()) {
                healthPermissionLauncher.launch(healthConnectManager.requiredPermissions)
            }
        }

        if (hasLocationPermission()) {
            liveSteps = 0
            liveAPs = 0
            isWalking = true
            val intent = Intent(context, WarWalkingService::class.java).setAction(WarWalkingService.ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        } else {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions += Manifest.permission.BLUETOOTH_SCAN
                permissions += Manifest.permission.BLUETOOTH_CONNECT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions += Manifest.permission.POST_NOTIFICATIONS
            }
            runtimePermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    fun stopWalk() {
        isWalking = false
        val intent = Intent(context, WarWalkingService::class.java).setAction(WarWalkingService.ACTION_STOP)
        ContextCompat.startForegroundService(context, intent)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Walk") },
                    label = { NavLabel("Walk") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { NavLabel("Profile") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { NavLabel("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { NavLabel("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> MainDashboardView(
                    currentStreak = currentStreak,
                    isWalking = isWalking,
                    liveSteps = liveSteps,
                    liveAPs = liveAPs,
                    onStartWalk = ::startWalk,
                    onStopWalk = ::stopWalk,
                )
                1 -> ProfileScreen(profileViewModel)
                2 -> HistoryScreen(historyViewModel)
                3 -> SettingsScreen(
                    viewModel = credentialViewModel,
                    scanSettings = scanSettings,
                    onScanSettingsChange = { scanSettings = it },
                )
            }
        }
    }
}

// Plain Text() here inherits the app-wide monospace typography (Theme.kt),
// which is meaningfully wider per character than a default sans font - at
// five tabs across, that was enough for "Settings" to wrap to a second line.
// A dedicated smaller, single-line style fixes it without giving up labels.
@Composable
private fun NavLabel(text: String) {
    Text(text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
