package com.warwalking.app.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
        val response = api.registerUser(UserRegisterRequest(username, email, apiName, apiToken))
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("Error ${response.code()}: ${response.errorBody()?.string()}")
        }
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

        val response = api.syncSession(
            userId = userIdBody,
            startTime = startBody,
            endTime = endBody,
            stepsCounted = stepsBody,
            apDiscovered = apsBody,
            file = filePart
        )

        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("Upload refused (${response.code()}): ${response.errorBody()?.string()}")
        }
    }

    suspend fun getLeaderboard(): Result<List<LeaderboardEntry>> = runCatching {
        val response = api.getLeaderboard()
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("Error ${response.code()}: ${response.errorBody()?.string()}")
        }
    }
}
