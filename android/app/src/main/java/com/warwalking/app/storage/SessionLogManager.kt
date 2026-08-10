package com.warwalking.app.storage

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionLogManager(private val context: Context) {

    private var activeFile: File? = null

    private val wigleTimestampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    /** Starts a fresh session file with the mandatory WigleWifi-1.4 header lines. */
    fun startNewSessionFile(): File? {
        val fileName = "warwalk_${Instant.now().epochSecond}.csv"
        val file = File(context.filesDir, fileName)
        activeFile = file

        return try {
            FileOutputStream(file, false).use { stream ->
                val header1 = "WigleWifi-1.4,appRelease=0.1.0,model=${Build.MODEL}," +
                    "device=${Build.DEVICE},display=${Build.DISPLAY},board=${Build.BOARD}," +
                    "brand=${Build.BRAND}\n"
                val header2 = "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude," +
                    "CurrentLongitude,AltitudeMeters,AccuracyMeters,Type\n"
                stream.write(header1.toByteArray())
                stream.write(header2.toByteArray())
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /** Appends one discovered network/beacon as a WigleWifi CSV row. */
    fun logNetworkRow(
        mac: String,
        ssid: String,
        capabilities: String,
        channel: Int,
        rssi: Int,
        lat: Double,
        lon: Double,
        alt: Double,
        accuracy: Float,
        type: String = "WIFI"
    ) {
        val file = activeFile ?: return
        if (!file.exists()) return

        val timestamp = wigleTimestampFormat.format(Instant.now())
        val row = listOf(
            mac, csvSafe(ssid), csvSafe(capabilities), timestamp, channel, rssi,
            lat, lon, alt, accuracy, type
        ).joinToString(",") + "\n"

        try {
            FileOutputStream(file, true).use { it.write(row.toByteArray()) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getFinalSessionFile(): File? = activeFile

    fun clearSessionCache() {
        activeFile?.takeIf { it.exists() }?.delete()
        activeFile = null
    }

    /** Quotes a CSV field if it contains a comma, quote, or newline (SSIDs can contain any of these). */
    private fun csvSafe(field: String): String {
        return if (field.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
