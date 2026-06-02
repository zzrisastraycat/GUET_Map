package com.example.guet_map.model

data class GuideStep(
    val id: Long = 0,
    val locationId: String,
    val stepNumber: Int,
    val description: String,
    val imageUrl: String
)
