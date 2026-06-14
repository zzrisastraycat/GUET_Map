package com.example.guet_map.module.social.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 天气缓存实体
 */
@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey
    val id: String,
    val temperature: Int,
    val feelsLike: Int,
    val humidity: Int,
    val windSpeed: Float,
    val windDirection: String,
    val weatherType: String,
    val description: String,
    val aqi: Int,
    val aqiLevel: String,
    val uvIndex: Int,
    val sunrise: Long,
    val sunset: Long,
    val hourlyForecast: String,   // JSON
    val alertMessage: String,
    val cachedAt: Long = System.currentTimeMillis()
)
