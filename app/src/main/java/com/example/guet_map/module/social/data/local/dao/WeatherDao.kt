package com.example.guet_map.module.social.data.local.dao

import androidx.room.*
import com.example.guet_map.module.social.data.local.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

/**
 * 天气缓存 DAO
 */
@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestWeather(): WeatherEntity?

    @Query("SELECT * FROM weather_cache ORDER BY cachedAt DESC LIMIT 1")
    fun observeLatestWeather(): Flow<WeatherEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("DELETE FROM weather_cache")
    suspend fun clearCache()

    @Query("DELETE FROM weather_cache WHERE cachedAt < :timestamp")
    suspend fun clearOldCache(timestamp: Long)
}
