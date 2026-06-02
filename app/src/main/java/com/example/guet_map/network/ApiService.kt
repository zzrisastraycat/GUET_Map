package com.example.guet_map.network

import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── 地点接口 ──────────────────────────────────────────────

    @GET("api/v1/locations")
    suspend fun getLocations(): List<Location>

    @GET("api/v1/locations")
    suspend fun getLocationsByCategory(
        @Query("category") category: String
    ): List<Location>

    @GET("api/v1/locations/{locationId}")
    suspend fun getLocationDetail(
        @Path("locationId") locationId: String
    ): Location

    // ── 图文指引接口 ──────────────────────────────────────────

    @GET("api/v1/locations/{locationId}/guides")
    suspend fun getGuideSteps(
        @Path("locationId") locationId: String
    ): List<GuideStep>

    @GET("api/v1/locations/{locationId}/guides")
    suspend fun getGuideStepsPaged(
        @Path("locationId") locationId: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): List<GuideStep>

    // ── UGC 上传接口 ──────────────────────────────────────────

    @Multipart
    @POST("api/v1/guides/upload")
    suspend fun uploadGuideStep(
        @Part("locationId") locationId: RequestBody,
        @Part("stepNumber") stepNumber: RequestBody,
        @Part("description") description: RequestBody,
        @Part image: MultipartBody.Part
    ): UploadResponse
}
