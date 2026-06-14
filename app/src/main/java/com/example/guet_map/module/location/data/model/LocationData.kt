package com.example.guet_map.module.location.data.model

/**
 * 定位数据模型
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 位置状态
 */
sealed class LocationState {
    data object Idle : LocationState()
    data object Loading : LocationState()
    data class Success(val location: LocationData) : LocationState()
    data class Error(val message: String) : LocationState()
}
