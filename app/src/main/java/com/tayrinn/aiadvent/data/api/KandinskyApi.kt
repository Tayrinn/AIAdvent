package com.tayrinn.aiadvent.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class KandinskyRequest(
    val type: String = "GENERATE",
    val style: String = "DEFAULT",
    val width: Int = 1024,
    val height: Int = 1024,
    val numImages: Int = 1,
    val negativePromptDecoder: String = "",
    val generateParams: GenerateParams
)

data class GenerateParams(
    val query: String
)

data class KandinskyResponse(
    val result: String,
    val imageUrl: String? = null,
    val error: String? = null
)

interface KandinskyApi {
    @POST("generate")
    suspend fun generateImage(@Body request: KandinskyRequest): KandinskyResponse
}
