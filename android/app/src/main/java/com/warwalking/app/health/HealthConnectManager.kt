package com.warwalking.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectManager(private val context: Context) {

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    // Lazily created - only ever touched after isAvailable is confirmed true.
    val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable) return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Sums verified step records for the exact walk window. Returns 0 if Health
     * Connect isn't installed/available or permission hasn't been granted -
     * callers should gate on [hasPermissions] before relying on a nonzero result.
     */
    suspend fun fetchStepsDelta(startTime: Instant, endTime: Instant): Long {
        if (!isAvailable) return 0L

        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            client.readRecords(request).records.sumOf { it.count }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
