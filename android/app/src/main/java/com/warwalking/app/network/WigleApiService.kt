package com.warwalking.app.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Direct client for api.wigle.net - no backend in between. Every call takes
 *  its own Basic Auth header since credentials are per-user, not global. */
interface WigleApiService {

    @GET("api/v2/profile/user")
    suspend fun getProfile(@Header("Authorization") auth: String): Response<WigleProfileResponse>

    @GET("api/v2/stats/user")
    suspend fun getUserStats(@Header("Authorization") auth: String): Response<WigleUserStatsResponse>

    @Multipart
    @POST("api/v2/file/upload")
    suspend fun uploadFile(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part
    ): Response<WigleUploadResponse>
}
