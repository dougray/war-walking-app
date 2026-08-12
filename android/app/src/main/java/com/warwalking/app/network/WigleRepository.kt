package com.warwalking.app.network

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

/** Thin Result-wrapping layer over WigleApiService - no caching, no retry policy. */
class WigleRepository(private val api: WigleApiService = WigleClient.apiService) {

    suspend fun verifyCredentials(apiName: String, apiToken: String): Result<WigleProfileResponse> = runCatching {
        unwrap(api.getProfile(basicAuth(apiName, apiToken)))
    }

    suspend fun fetchUserStats(apiName: String, apiToken: String): Result<WigleUserStatsResponse> = runCatching {
        unwrap(api.getUserStats(basicAuth(apiName, apiToken)))
    }

    suspend fun uploadSessionFile(apiName: String, apiToken: String, file: File): Result<WigleUploadResponse> = runCatching {
        val fileRequestBody = file.asRequestBody("text/csv".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)
        unwrap(api.uploadFile(basicAuth(apiName, apiToken), filePart))
    }

    private fun basicAuth(apiName: String, apiToken: String): String = Credentials.basic(apiName, apiToken)

    private fun <T> unwrap(response: Response<T>): T {
        if (response.isSuccessful && response.body() != null) return response.body()!!
        throw Exception("WiGLE error ${response.code()}: ${response.errorBody()?.string()}")
    }
}
