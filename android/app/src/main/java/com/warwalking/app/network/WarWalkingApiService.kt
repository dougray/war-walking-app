package com.warwalking.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface WarWalkingApiService {

    @POST("api/users/register")
    suspend fun registerUser(@Body request: UserRegisterRequest): Response<UserRegisterResponse>

    @Multipart
    @POST("api/sessions/sync")
    suspend fun syncSession(
        @Part("user_id") userId: RequestBody,
        @Part("start_time") startTime: RequestBody,
        @Part("end_time") endTime: RequestBody,
        @Part("steps_counted") stepsCounted: RequestBody,
        @Part("ap_discovered") apDiscovered: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SyncSessionResponse>

    @GET("api/leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>
}
