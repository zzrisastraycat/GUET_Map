package com.example.guet_map.repository

import com.example.guet_map.data.UserPrefs
import com.example.guet_map.local.dao.FavoriteDao
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.entity.FavoriteEntity
import com.example.guet_map.model.FavoriteRequest
import com.example.guet_map.model.Location
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val apiService: ApiService,
    private val favoriteDao: FavoriteDao,
    private val locationDao: LocationDao,
    private val userPrefs: UserPrefs
) {
    private val activeUserId = MutableStateFlow(currentUserId())

    init {
        activeUserId.value = currentUserId()
    }

    private fun currentUserId(): String =
        userPrefs.userId.ifBlank { UserPrefs.GUEST_USER_ID }

    fun switchUser(userId: String) {
        activeUserId.value = userId.ifBlank { UserPrefs.GUEST_USER_ID }
    }

    fun observeFavoriteIds(): Flow<Set<String>> =
        activeUserId.flatMapLatest { uid ->
            favoriteDao.observeFavoriteIds(uid).map { it.toSet() }
        }

    fun observeFavoriteLocations(): Flow<List<Location>> =
        activeUserId.flatMapLatest { uid ->
            combine(
                favoriteDao.observeAll(uid),
                locationDao.getAllLocations()
            ) { favorites, locations ->
                val byId = locations.associateBy { it.locationId }
                favorites.mapNotNull { fav ->
                    byId[fav.locationId]?.let { entity ->
                        Location(
                            locationId = entity.locationId,
                            name = entity.name,
                            latitude = entity.latitude,
                            longitude = entity.longitude,
                            category = entity.category,
                            rating = entity.rating,
                            openingHours = entity.openingHours,
                            imageUrl = entity.imageUrl,
                            hasGuide = entity.hasGuide
                        )
                    }
                }
            }
        }

    suspend fun isFavorite(locationId: String): Boolean =
        favoriteDao.isFavorite(activeUserId.value, locationId) > 0

    suspend fun toggleFavorite(location: Location): Boolean {
        val uid = activeUserId.value
        val currentlyFavorite = favoriteDao.isFavorite(uid, location.locationId) > 0
        return if (currentlyFavorite) {
            removeFavorite(location.locationId)
            false
        } else {
            addFavorite(location)
            true
        }
    }

    suspend fun addFavorite(location: Location) {
        val uid = activeUserId.value
        try {
            apiService.addFavorite(FavoriteRequest(location.locationId))
        } catch (_: Exception) {
        }
        favoriteDao.insert(FavoriteEntity(userId = uid, locationId = location.locationId))
    }

    suspend fun removeFavorite(locationId: String) {
        val uid = activeUserId.value
        try {
            apiService.removeFavorite(locationId)
        } catch (_: Exception) {
        }
        favoriteDao.delete(uid, locationId)
    }

    suspend fun syncFromServer() {
        val uid = activeUserId.value
        if (uid == UserPrefs.GUEST_USER_ID) return
        try {
            val remote = apiService.getFavorites()
            favoriteDao.deleteAllForUser(uid)
            remote.forEach { loc ->
                favoriteDao.insert(FavoriteEntity(userId = uid, locationId = loc.locationId))
                locationDao.insertAll(listOf(loc.toEntity()))
            }
        } catch (_: Exception) {
        }
    }

    suspend fun enrichFavoriteFromCache(locationId: String): Location? =
        locationDao.getLocationById(locationId)?.let { entity ->
            Location(
                locationId = entity.locationId,
                name = entity.name,
                latitude = entity.latitude,
                longitude = entity.longitude,
                category = entity.category,
                rating = entity.rating,
                openingHours = entity.openingHours,
                imageUrl = entity.imageUrl,
                hasGuide = entity.hasGuide
            )
        }

    private fun Location.toEntity() = com.example.guet_map.local.entity.LocationEntity(
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
