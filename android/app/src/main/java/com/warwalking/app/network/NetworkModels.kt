package com.warwalking.app.network

import com.google.gson.annotations.SerializedName

data class UserRegisterRequest(
    val username: String,
    val email: String,
    @SerializedName("wigle_api_name") val wigleApiName: String,
    @SerializedName("wigle_api_token") val wigleApiToken: String
)

data class UserRegisterResponse(
    val status: String,
    @SerializedName("user_id") val userId: Int,
    val username: String
)

data class SyncSessionResponse(
    val status: String,
    @SerializedName("session_id") val sessionId: Int,
    val message: String
)

data class LeaderboardEntry(
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("total_steps") val totalSteps: Long,
    @SerializedName("total_aps_mapped") val totalApsMapped: Long,
    @SerializedName("total_score") val totalScore: Long,
    @SerializedName("total_walks") val totalWalks: Int
)
