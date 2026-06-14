package com.example.guet_map.module.social.domain.usecase

import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.repository.WeatherRepository
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取天气用例
 */
class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(): Resource<Weather> {
        return weatherRepository.getWeather()
    }

    fun observe(): Flow<Weather?> {
        return weatherRepository.observeWeather()
    }
}

/**
 * 刷新天气用例
 */
class RefreshWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(): Resource<Weather> {
        return weatherRepository.refreshWeather()
    }
}
