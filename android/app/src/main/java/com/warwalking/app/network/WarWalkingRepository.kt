package com.warwalking.app.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.time.Instant

/** Thin Result-wrapping layer over WarWalkingApiService - no caching, no retry policy. */
class WarWalkingRepository(private val api: WarWalkingApiService = NetworkClient.apiService) {

    suspend fun register(
        username: String,
        email: String,
        apiName: String,
        apiToken: String
    ): Result<UserRegisterResponse> = runCatching {
        unwrap(api.registerUser(UserRegisterRequest(username, email, apiName, apiToken)))
    }

    suspend fun syncSessionData(
        userId: Int,
        startTime: Instant,
        endTime: Instant,
        steps: Int,
        apsFound: Int,
        logFile: File
    ): Result<SyncSessionResponse> = runCatching {
        val textType = "text/plain".toMediaTypeOrNull()
        val userIdBody = userId.toString().toRequestBody(textType)
        val startBody = startTime.toString().toRequestBody(textType)
        val endBody = endTime.toString().toRequestBody(textType)
        val stepsBody = steps.toString().toRequestBody(textType)
        val apsBody = apsFound.toString().toRequestBody(textType)

        val fileRequestBody = logFile.asRequestBody("text/csv".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", logFile.name, fileRequestBody)

        unwrap(
            api.syncSession(
                userId = userIdBody,
                startTime = startBody,
                endTime = endBody,
                stepsCounted = stepsBody,
                apDiscovered = apsBody,
                file = filePart
            )
        )
    }

    suspend fun getLeaderboard(): Result<List<LeaderboardEntry>> = runCatching {
        unwrap(api.getLeaderboard())
    }

    suspend fun getUserSessions(userId: Int): Result<List<SessionSummary>> = runCatching {
        unwrap(api.getUserSessions(userId))
    }

    suspend fun updateSession(
        sessionId: Int,
        userId: Int,
        title: String? = null,
        isPublic: Boolean? = null
    ): Result<SessionUpdateResponse> = runCatching {
        unwrap(api.updateSession(sessionId, SessionUpdateRequest(userId, title, isPublic)))
    }

    suspend fun getFeed(viewerUserId: Int?): Result<List<FeedItem>> = runCatching {
        unwrap(api.getFeed(viewerUserId))
    }

    suspend fun giveKudos(sessionId: Int, userId: Int): Result<KudosResponse> = runCatching {
        unwrap(api.giveKudos(sessionId, KudosRequest(userId)))
    }

    suspend fun removeKudos(sessionId: Int, userId: Int): Result<KudosResponse> = runCatching {
        unwrap(api.removeKudos(sessionId, userId))
    }

    suspend fun getComments(sessionId: Int): Result<List<SessionComment>> = runCatching {
        unwrap(api.getComments(sessionId))
    }

    suspend fun addComment(sessionId: Int, userId: Int, body: String): Result<CommentCreateResponse> = runCatching {
        unwrap(api.addComment(sessionId, CommentCreateRequest(userId, body)))
    }

    private fun <T> unwrap(response: Response<T>): T {
        if (response.isSuccessful && response.body() != null) return response.body()!!
        throw Exception("Error ${response.code()}: ${response.errorBody()?.string()}")
    }
}
