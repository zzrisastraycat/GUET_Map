package com.example.guet_map.module.location.domain.usecase

import com.example.guet_map.module.location.data.model.LocationData
import com.example.guet_map.module.location.data.model.LocationState
import com.example.guet_map.module.location.domain.service.LocationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取当前位置用例
 */
class GetCurrentLocationUseCase @Inject constructor(
    private val locationService: LocationService
) {
    suspend operator fun invoke(): LocationState {
        return locationService.getCurrentLocation()
    }
}

/**
 * 启动持续定位用例
 */
class StartLocationUpdatesUseCase @Inject constructor(
    private val locationService: LocationService
) {
    operator fun invoke(): Flow<LocationData> {
        return locationService.startLocationUpdates()
    }
}

/**
 * 停止定位用例
 */
class StopLocationUpdatesUseCase @Inject constructor(
    private val locationService: LocationService
) {
    operator fun invoke() {
        locationService.stopLocationUpdates()
    }
}
