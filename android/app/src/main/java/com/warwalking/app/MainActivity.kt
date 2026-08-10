package com.warwalking.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import com.warwalking.app.health.HealthConnectManager
import com.warwalking.app.service.WarWalkingService
import com.warwalking.app.ui.EventBoardScreen
import com.warwalking.app.ui.HistoryScreen
import com.warwalking.app.ui.MainDashboardView
import com.warwalking.app.ui.RegisterViewModel
import com.warwalking.app.ui.ScanSettings
import com.warwalking.app.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
    val registerViewModel = remember { RegisterViewModel() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isWalking by remember { mutableStateOf(false) }
    var liveSteps by remember { mutableIntStateOf(0) }
    var liveAPs by remember { mutableIntStateOf(0) }
    // TODO: back with a real streaks endpoint once GET /api/users/{id}/streak exists.
    var currentStreak by remember { mutableIntStateOf(0) }
    var scanSettings by remember { mutableStateOf(ScanSettings()) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { /* granted set ignored here; checked again lazily via hasPermissions() before each session */ }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* WarWalkingService checks permissions again defensively before using each API */ }

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

    fun requestRuntimePermissions() {
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

    fun startWalk() {
        requestRuntimePermissions()
        coroutineScope.launch {
            if (healthConnectManager.isAvailable && !healthConnectManager.hasPermissions()) {
                healthPermissionLauncher.launch(healthConnectManager.requiredPermissions)
            }
        }
        liveSteps = 0
        liveAPs = 0
        isWalking = true
        val intent = Intent(context, WarWalkingService::class.java).setAction(WarWalkingService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
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
                    label = { Text("Walk") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Events") },
                    label = { Text("Events") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
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
                1 -> EventBoardScreen()
                2 -> HistoryScreen()
                3 -> SettingsScreen(
                    viewModel = registerViewModel,
                    scanSettings = scanSettings,
                    onScanSettingsChange = { scanSettings = it },
                )
            }
        }
    }
}
