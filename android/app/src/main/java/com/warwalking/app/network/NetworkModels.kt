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

// --- Social feed ---
// Deliberately carries only aggregate stats + the walker's own caption -
// never scanned SSIDs/MACs/coordinates. See backend/migrations/003_social.sql.

data class FeedItem(
    @SerializedName("session_id") val sessionId: Int,
    val title: String?,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("steps_counted") val stepsCounted: Int,
    @SerializedName("ap_discovered") val apDiscovered: Int,
    @SerializedName("points_earned") val pointsEarned: Long,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("kudos_count") val kudosCount: Int,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("viewer_has_kudos") val viewerHasKudos: Boolean
)

data class SessionSummary(
    @SerializedName("session_id") val sessionId: Int,
    val title: String?,
    @SerializedName("is_public") val isPublic: Boolean,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("steps_counted") val stepsCounted: Int,
    @SerializedName("ap_discovered") val apDiscovered: Int,
    @SerializedName("points_earned") val pointsEarned: Long,
    @SerializedName("wigle_file_id") val wigleFileId: String?,
    @SerializedName("created_at") val createdAt: String
)

data class SessionComment(
    @SerializedName("comment_id") val commentId: Int,
    val body: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("user_id") val userId: Int,
    val username: String
)

data class SessionUpdateRequest(
    @SerializedName("user_id") val userId: Int,
    val title: String? = null,
    @SerializedName("is_public") val isPublic: Boolean? = null
)

data class SessionUpdateResponse(
    val status: String,
    @SerializedName("session_id") val sessionId: Int
)

data class KudosRequest(@SerializedName("user_id") val userId: Int)

data class KudosResponse(
    val status: String,
    @SerializedName("kudos_count") val kudosCount: Int
)

data class CommentCreateRequest(
    @SerializedName("user_id") val userId: Int,
    val body: String
)

data class CommentCreateResponse(
    val status: String,
    @SerializedName("comment_id") val commentId: Int
)
