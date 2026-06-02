package com.example.guet_map.repository

import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.entity.LocationEntity
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao
) {

    /**
     * SSOT: 网络优先 → 缓存到 Room → 网络失败时回退缓存。
     */
    fun getLocations(): Flow<Resource<List<Location>>> = flow {
        emit(Resource.Loading)

        val hasCached = locationDao.count() > 0

        try {
            val remote = apiService.getLocations()
            locationDao.deleteAll()
            locationDao.insertAll(remote.map { it.toEntity() })
            emit(Resource.Success(remote))
        } catch (e: IOException) {
            if (hasCached) {
                val cached = locationDao.getAllLocations().first()
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("网络不可用: ${e.localizedMessage}"))
            }
        } catch (e: Exception) {
            if (hasCached) {
                val cached = locationDao.getAllLocations().first()
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("加载失败: ${e.localizedMessage}"))
            }
        }
    }

    fun getLocationsByCategory(category: String): Flow<Resource<List<Location>>> = flow {
        emit(Resource.Loading)

        try {
            val remote = apiService.getLocationsByCategory(category)
            locationDao.insertAll(remote.map { it.toEntity() })
            emit(Resource.Success(remote))
        } catch (e: Exception) {
            val cached = locationDao.getLocationsByCategory(category).first()
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("加载失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 观察 Room 中的缓存数据（实时更新）。
     */
    fun observeCachedLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations()
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun observeCachedLocationsByCategory(category: String): Flow<List<Location>> {
        return locationDao.getLocationsByCategory(category)
            .map { entities -> entities.map { it.toDomain() } }
    }

    private fun Location.toEntity() = LocationEntity(
        locationId = locationId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = category,
        rating = rating,
        openingHours = openingHours,
        imageUrl = imageUrl,
        hasGuide = hasGuide
    )

    private fun LocationEntity.toDomain() = Location(
        locationId = locationId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = category,
        rating = rating,
        openingHours = openingHours,
        imageUrl = imageUrl,
        hasGuide = hasGuide
    )
}
