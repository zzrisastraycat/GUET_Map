package com.example.guet_map.model

data class Location(
    val locationId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val rating: Float = 0f,
    val openingHours: String = "",
    val imageUrl: String = "",
    val hasGuide: Boolean = false,
    val address: String = "",
    val phone: String = "",
    val description: String = "",
    val poiId: String = ""
)
