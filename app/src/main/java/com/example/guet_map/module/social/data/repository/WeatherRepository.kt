package com.example.guet_map.module.social.data.repository

import com.example.guet_map.module.social.data.local.dao.WeatherDao
import com.example.guet_map.module.social.data.local.entity.WeatherEntity
import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherType
import com.example.guet_map.model.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天气仓库
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherDao: WeatherDao,
    private val gson: Gson
) {

    fun observeWeather(): Flow<Weather?> {
        return weatherDao.observeLatestWeather().map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun getWeather(): Resource<Weather> {
        return try {
            // 先检查缓存
            weatherDao.getLatestWeather()?.let { cached ->
                val cacheAge = System.currentTimeMillis() - cached.cachedAt
                // 缓存 30 分钟内有效
                if (cacheAge < 30 * 60 * 1000) {
                    return Resource.Success(cached.toDomain())
                }
            }

            // TODO: 调用真实天气 API
            // 目前返回模拟数据
            val mockWeather = generateMockWeather()
            weatherDao.insertWeather(mockWeather.toEntity())
            Resource.Success(mockWeather)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "获取天气失败")
        }
    }

    suspend fun refreshWeather(): Resource<Weather> {
        return try {
            // TODO: 调用真实天气 API
            val mockWeather = generateMockWeather()
            weatherDao.insertWeather(mockWeather.toEntity())
            Resource.Success(mockWeather)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "刷新天气失败")
        }
    }

    private fun generateMockWeather(): Weather {
        val hourlyForecast = (0..23).map { hour ->
            HourlyWeather(
                hour = hour,
                temperature = (20..30).random(),
                weatherType = WeatherType.SUNNY,
                precipitation = (0..30).random()
            )
        }

        return Weather(
            id = UUID.randomUUID().toString(),
            temperature = 26,
            feelsLike = 28,
            humidity = 65,
            windSpeed = 2.5f,
            windDirection = "东南风",
            weatherType = WeatherType.SUNNY,
            description = "晴转多云",
            aqi = 45,
            aqiLevel = "优",
            uvIndex = 6,
            sunrise = System.currentTimeMillis() - 8 * 60 * 60 * 1000,
            sunset = System.currentTimeMillis() + 6 * 60 * 60 * 1000,
            hourlyForecast = hourlyForecast,
            alertMessage = null
        )
    }

    private fun WeatherEntity.toDomain() = Weather(
        id = id,
        temperature = temperature,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = WeatherType.valueOf(weatherType),
        description = description,
        aqi = aqi,
        aqiLevel = aqiLevel,
        uvIndex = uvIndex,
        sunrise = sunrise,
        sunset = sunset,
        hourlyForecast = gson.fromJson(hourlyForecast, object : TypeToken<List<HourlyWeather>>() {}.type) ?: emptyList(),
        alertMessage = alertMessage
    )

    private fun Weather.toEntity() = WeatherEntity(
        id = id,
        temperature = temperature,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = weatherType.name,
        description = description,
        aqi = aqi ?: 0,
        aqiLevel = aqiLevel ?: "",
        uvIndex = uvIndex ?: 0,
        sunrise = sunrise,
        sunset = sunset,
        hourlyForecast = gson.toJson(hourlyForecast),
        alertMessage = alertMessage ?: ""
    )
}
