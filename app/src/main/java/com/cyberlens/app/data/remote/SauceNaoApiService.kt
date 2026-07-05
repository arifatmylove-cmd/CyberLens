package com.cyberlens.app.data.remote

import com.cyberlens.app.data.remote.dto.SauceNaoResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SauceNaoApiService {
    @GET("search.php")
    suspend fun searchByUrl(
        @Query("api_key") apiKey: String,
        @Query("url") imageUrl: String,
        @Query("output_type") outputType: Int = 2,
        @Query("numres") numResults: Int = 10,
        @Query("minsim") minSimilarity: Int = 50
    ): Response<SauceNaoResponseDto>

    @Multipart
    @POST("search.php")
    suspend fun searchByFile(
        @Query("api_key") apiKey: String,
        @Query("output_type") outputType: Int = 2,
        @Query("numres") numResults: Int = 10,
        @Part file: MultipartBody.Part
    ): Response<SauceNaoResponseDto>
}
