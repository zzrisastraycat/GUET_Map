package com.example.guet_map.core.map.model

import com.amap.api.maps.model.LatLng

/**
 * 位置服务接口
 * 定义地点数据提供者的标准接口
 */
interface LocationProvider {

    /**
     * 根据 ID 获取地点信息
     */
    suspend fun getLocationById(id: String): Location?

    /**
     * 获取所有地点
     */
    fun getAllLocations(): List<Location>

    /**
     * 按分类获取地点
     */
    fun getLocationsByCategory(category: String): List<Location>

    /**
     * 搜索地点
     */
    fun searchLocations(keyword: String): List<Location>

    /**
     * 观察地点列表变化
     */
    // fun observeLocations(): Flow<List<Location>>

    /**
     * 计算两点之间的距离（米）
     */
    fun calculateDistance(from: LatLng, to: LatLng): Float

    /**
     * 查找最近的地点
     */
    fun findNearestLocation(from: LatLng, locations: List<Location>): Location?
}
