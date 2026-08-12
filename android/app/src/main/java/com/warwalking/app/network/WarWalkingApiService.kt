package com.warwalking.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("api/sessions")
    suspend fun getUserSessions(@Query("user_id") userId: Int): Response<List<SessionSummary>>

    @PATCH("api/sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: Int,
        @Body request: SessionUpdateRequest
    ): Response<SessionUpdateResponse>

    @GET("api/feed")
    suspend fun getFeed(@Query("viewer_user_id") viewerUserId: Int?): Response<List<FeedItem>>

    @POST("api/sessions/{sessionId}/kudos")
    suspend fun giveKudos(
        @Path("sessionId") sessionId: Int,
        @Body request: KudosRequest
    ): Response<KudosResponse>

    @DELETE("api/sessions/{sessionId}/kudos")
    suspend fun removeKudos(
        @Path("sessionId") sessionId: Int,
        @Query("user_id") userId: Int
    ): Response<KudosResponse>

    @GET("api/sessions/{sessionId}/comments")
    suspend fun getComments(@Path("sessionId") sessionId: Int): Response<List<SessionComment>>

    @POST("api/sessions/{sessionId}/comments")
    suspend fun addComment(
        @Path("sessionId") sessionId: Int,
        @Body request: CommentCreateRequest
    ): Response<CommentCreateResponse>
}
