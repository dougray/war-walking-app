package com.warwalking.app.network

import com.google.gson.annotations.SerializedName

data class WigleProfileResponse(
    val success: Boolean = true,
    val userid: String? = null,
    val email: String? = null
)

data class WigleUserStatsResponse(
    val success: Boolean = true,
    @SerializedName("imageBadgeUrl") val imageBadgeUrl: String? = null,
    val rank: Int = 0,
    @SerializedName("monthRank") val monthRank: Int = 0,
    val user: String? = null,
    val statistics: WigleUserStandings? = null
)

data class WigleUserStandings(
    val rank: Int = 0,
    @SerializedName("monthRank") val monthRank: Int = 0,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("discoveredWiFiGPS") val discoveredWiFiGPS: Int = 0,
    @SerializedName("discoveredWiFi") val discoveredWiFi: Int = 0,
    @SerializedName("discoveredCellGPS") val discoveredCellGPS: Int = 0,
    @SerializedName("discoveredCell") val discoveredCell: Int = 0,
    @SerializedName("discoveredBtGPS") val discoveredBtGPS: Int = 0,
    @SerializedName("discoveredBt") val discoveredBt: Int = 0,
    @SerializedName("eventMonthCount") val eventMonthCount: Int = 0,
    @SerializedName("eventPrevMonthCount") val eventPrevMonthCount: Int = 0,
    @SerializedName("totalWiFiLocations") val totalWiFiLocations: Long = 0
)

data class WigleUploadResponse(
    val success: Boolean = true,
    val transid: String? = null
)
