package com.example.guet_map.model

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val pointsAwarded: Int = 0
)
