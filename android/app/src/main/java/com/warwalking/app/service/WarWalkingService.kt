package com.warwalking.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.warwalking.app.WigleCredentialStore
import com.warwalking.app.data.AppDatabase
import com.warwalking.app.data.WalkSessionEntity
import com.warwalking.app.health.HealthConnectManager
import com.warwalking.app.network.WigleRepository
import com.warwalking.app.storage.SessionLogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Foreground service that owns the full lifecycle of one walking session:
 * start scanning -> keep the radio + GPS alive while the screen is off ->
 * on stop, pull verified steps from Health Connect, sync the session to the
 * backend, then shut itself down. Session file ownership lives here
 * (not in the Activity) so there's a single source of truth for "what got
 * scanned this session."
 */
class WarWalkingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "com.warwalking.app.action.START"
        const val ACTION_STOP = "com.warwalking.app.action.STOP"

        const val ACTION_UI_UPDATE = "com.warwalking.app.UI_UPDATE"
        const val EXTRA_AP_COUNT = "EXTRA_AP_COUNT"
        const val EXTRA_LIVE_STEPS = "EXTRA_LIVE_STEPS"
        const val ACTION_SESSION_SYNC_RESULT = "com.warwalking.app.SESSION_SYNC_RESULT"
        const val EXTRA_SYNC_MESSAGE = "EXTRA_SYNC_MESSAGE"

        private const val CHANNEL_ID = "war_walking_channel"
        private const val NOTIFICATION_ID = 4224
        private const val SCAN_INTERVAL_MS = 10_000L
        private const val WAKELOCK_SAFETY_TIMEOUT_MS = 6 * 60 * 60 * 1000L // 6h safety net
    }

    private lateinit var wifiManager: WifiManager
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var sessionLogManager: SessionLogManager
    private lateinit var healthConnectManager: HealthConnectManager
    private val wigleRepository = WigleRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var bluetoothLeScanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var receiverRegistered = false

    private val handler = Handler(Looper.getMainLooper())
    private var lastLocation: Location? = null
    private var stepsAtSessionStart = -1
    private var sessionStartTime: Instant? = null
    private var seenMacs = mutableSetOf<String>()

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                processWifiResults()
            }
        }
    }

    private val locationListener = LocationListener { location -> lastLocation = location }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            logBleResult(result)
        }
    }

    private val stepCounterSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scanRunnable = object : Runnable {
        override fun run() {
            wifiManager.startScan() // return value ignored: false just means this cycle was throttled
            handler.postDelayed(this, SCAN_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sessionLogManager = SessionLogManager(applicationContext)
        healthConnectManager = HealthConnectManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSessionAndSync()
            else -> startSession()
        }
        return START_STICKY
    }

    private fun startSession() {
        startForeground(NOTIFICATION_ID, buildNotification("Scanning wireless terrain and tracking steps..."))

        sessionStartTime = Instant.now()
        stepsAtSessionStart = -1
        seenMacs = mutableSetOf()
        sessionLogManager.startNewSessionFile()

        acquireWakeLock()
        registerWifiReceiver()
        startLocationUpdates()
        startStepCounter()
        startBleScan()
        handler.post(scanRunnable)
    }

    private fun stopSessionAndSync() {
        val startTime = sessionStartTime ?: Instant.now()
        val endTime = Instant.now()

        handler.removeCallbacks(scanRunnable)
        locationManager.removeUpdates(locationListener)
        sensorManager.unregisterListener(this)
        stopBleScan()
        unregisterWifiReceiverIfNeeded()

        startForeground(NOTIFICATION_ID, buildNotification("Saving session..."))

        val apCount = seenMacs.size
        val logFile = sessionLogManager.getFinalSessionFile()

        serviceScope.launch {
            try {
                // Without a Health Connect grant we have no verified count, so score 0
                // rather than trust the raw step-counter sensor (which is what the
                // Exploration Index is specifically designed to prevent cheating around).
                val verifiedSteps = if (healthConnectManager.hasPermissions()) {
                    healthConnectManager.fetchStepsDelta(startTime, endTime).toInt()
                } else {
                    0
                }

                // The on-device database is the source of truth now, not a backend
                // account - history/streaks work the same whether or not WiGLE
                // credentials are configured.
                val dao = AppDatabase.get(applicationContext).walkSessionDao()
                var sessionEntity = WalkSessionEntity(
                    startTime = startTime.toEpochMilli(),
                    endTime = endTime.toEpochMilli(),
                    stepsCounted = verifiedSteps,
                    apDiscovered = apCount,
                )
                sessionEntity = sessionEntity.copy(id = dao.insert(sessionEntity))

                val apiName = WigleCredentialStore.getApiName(applicationContext)
                val apiToken = WigleCredentialStore.getApiToken(applicationContext)

                val resultMessage = when {
                    apiName == null || apiToken == null ->
                        "Saved locally - add your WiGLE API keys in Settings to upload."
                    logFile == null || !logFile.exists() ->
                        "Saved locally - no scan data captured this session."
                    else -> {
                        val result = wigleRepository.uploadSessionFile(apiName, apiToken, logFile)
                        result.fold(
                            onSuccess = { response ->
                                dao.update(sessionEntity.copy(wigleTransId = response.transid ?: "UPLOADED"))
                                "Synced to WiGLE: $verifiedSteps steps, $apCount APs."
                            },
                            onFailure = { "Saved locally - WiGLE upload failed: ${it.message}" }
                        )
                    }
                }

                sessionLogManager.clearSessionCache()
                broadcastSyncResult(resultMessage)
            } finally {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun broadcastSyncResult(message: String) {
        sendBroadcast(Intent(ACTION_SESSION_SYNC_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_SYNC_MESSAGE, message)
        })
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WarWalking::ScanWakeLock").apply {
            acquire(WAKELOCK_SAFETY_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun registerWifiReceiver() {
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(wifiScanReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterWifiReceiverIfNeeded() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(wifiScanReceiver) }
            receiverRegistered = false
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WarWalker Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setOngoing(true)
            .build()

    private fun hasPermission(permission: String) =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun startLocationUpdates() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (!locationManager.isProviderEnabled(provider)) continue
            // A live fix can take a while (or never arrive, stationary/indoors), and
            // requestLocationUpdates' minDistance filter means that silence is normal,
            // not a bug - seed from any prior fix so early scan rows aren't 0.0,0.0.
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(provider)
            }
            locationManager.requestLocationUpdates(provider, 5_000L, 5f, locationListener)
        }
    }

    private fun startStepCounter() {
        stepCounterSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    private fun startBleScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        if (adapter?.isEnabled != true) return
        bluetoothLeScanner = adapter.bluetoothLeScanner
        bluetoothLeScanner?.startScan(bleScanCallback)
    }

    private fun stopBleScan() {
        if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            runCatching { bluetoothLeScanner?.stopScan(bleScanCallback) }
        }
        bluetoothLeScanner = null
    }

    private fun processWifiResults() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        val results = wifiManager.scanResults
        val loc = lastLocation

        for (result in results) {
            seenMacs.add(result.BSSID)
            sessionLogManager.logNetworkRow(
                mac = result.BSSID,
                ssid = result.SSID,
                capabilities = result.capabilities,
                channel = channelFromFrequency(result.frequency),
                rssi = result.level,
                lat = loc?.latitude ?: 0.0,
                lon = loc?.longitude ?: 0.0,
                alt = loc?.altitude ?: 0.0,
                accuracy = loc?.accuracy ?: 0f,
                type = "WIFI"
            )
        }
        broadcastUiUpdate(apCount = seenMacs.size)
    }

    private fun logBleResult(result: ScanResult) {
        val mac = result.device.address ?: return
        if (!seenMacs.add(mac)) return // BLE advertises far more often than Wi-Fi beacons; de-dupe per session

        val loc = lastLocation
        sessionLogManager.logNetworkRow(
            mac = mac,
            ssid = result.scanRecord?.deviceName ?: "",
            capabilities = "[BLE]",
            channel = 0,
            rssi = result.rssi,
            lat = loc?.latitude ?: 0.0,
            lon = loc?.longitude ?: 0.0,
            alt = loc?.altitude ?: 0.0,
            accuracy = loc?.accuracy ?: 0f,
            type = "BLE"
        )
        broadcastUiUpdate(apCount = seenMacs.size)
    }

    private fun channelFromFrequency(freqMhz: Int): Int = when {
        freqMhz in 2412..2484 -> (freqMhz - 2412) / 5 + 1
        freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
        else -> 0
    }

    private fun broadcastUiUpdate(apCount: Int? = null, liveSteps: Int? = null) {
        val intent = Intent(ACTION_UI_UPDATE).apply {
            setPackage(packageName)
            apCount?.let { putExtra(EXTRA_AP_COUNT, it) }
            liveSteps?.let { putExtra(EXTRA_LIVE_STEPS, it) }
        }
        sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSinceBoot = event.values[0].toInt()
        if (stepsAtSessionStart < 0) stepsAtSessionStart = totalSinceBoot
        broadcastUiUpdate(liveSteps = totalSinceBoot - stepsAtSessionStart)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(scanRunnable)
        unregisterWifiReceiverIfNeeded()
        locationManager.removeUpdates(locationListener)
        sensorManager.unregisterListener(this)
        stopBleScan()
        releaseWakeLock()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "WarWalker Engine Channel", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
    }
}
