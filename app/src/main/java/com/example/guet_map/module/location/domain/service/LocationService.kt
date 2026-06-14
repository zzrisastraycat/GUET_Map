package com.example.guet_map.module.location.domain.service

import com.example.guet_map.module.location.data.model.LocationData
import com.example.guet_map.module.location.data.model.LocationState
import kotlinx.coroutines.flow.Flow

/**
 * 定位服务接口
 */
interface LocationService {

    /**
     * 获取单次定位
     */
    suspend fun getCurrentLocation(): LocationState

    /**
     * 启动持续定位
     */
    fun startLocationUpdates(): Flow<LocationData>

    /**
     * 停止定位
     */
    fun stopLocationUpdates()

    /**
     * 是否正在定位
     */
    fun isLocating(): Boolean

    /**
     * 获取最后已知位置
     */
    fun getLastKnownLocation(): LocationData?
}
