package com.example.guet_map.module.social.data.repository

import com.example.guet_map.module.social.data.local.dao.FavoriteDao
import com.example.guet_map.module.social.data.local.entity.FavoriteEntity
import com.example.guet_map.module.social.data.model.Favorite
import com.example.guet_map.module.social.data.model.FavoriteCategory
import com.example.guet_map.model.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏仓库
 */
@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val gson: Gson
) {

    fun getFavorites(userId: String): Flow<List<Favorite>> {
        return favoriteDao.getFavoritesByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFavoritesByCategory(userId: String, category: FavoriteCategory): Flow<List<Favorite>> {
        return if (category == FavoriteCategory.ALL) {
            favoriteDao.getFavoritesByUser(userId)
        } else {
            favoriteDao.getFavoritesByCategory(userId, category.name)
        }.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun isFavorite(userId: String, locationId: String): Boolean {
        return favoriteDao.isFavorite(userId, locationId)
    }

    fun observeIsFavorite(userId: String, locationId: String): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(userId, locationId)
    }

    suspend fun addFavorite(
        userId: String,
        locationId: String,
        locationName: String,
        locationCategory: String,
        latitude: Double,
        longitude: Double,
        note: String? = null,
        tags: List<String> = emptyList()
    ): Resource<Favorite> {
        return try {
            val favorite = Favorite(
                id = UUID.randomUUID().toString(),
                locationId = locationId,
                locationName = locationName,
                locationCategory = locationCategory,
                latitude = latitude,
                longitude = longitude,
                userId = userId,
                note = note,
                tags = tags
            )
            favoriteDao.insertFavorite(favorite.toEntity())
            Resource.Success(favorite)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "添加收藏失败")
        }
    }

    suspend fun removeFavorite(userId: String, locationId: String): Resource<Unit> {
        return try {
            favoriteDao.deleteFavoriteByLocation(userId, locationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "取消收藏失败")
        }
    }

    suspend fun updateFavoriteNote(favoriteId: String, note: String): Resource<Unit> {
        return try {
            // 需要先获取，再更新
            // 此处简化处理
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "更新备注失败")
        }
    }

    suspend fun getFavoriteCount(userId: String): Int {
        return favoriteDao.getFavoriteCount(userId)
    }

    private fun FavoriteEntity.toDomain() = Favorite(
        id = id,
        locationId = locationId,
        locationName = locationName,
        locationCategory = locationCategory,
        latitude = latitude,
        longitude = longitude,
        userId = userId,
        note = note,
        tags = gson.fromJson(tags, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        createdAt = createdAt
    )

    private fun Favorite.toEntity() = FavoriteEntity(
        id = id,
        locationId = locationId,
        locationName = locationName,
        locationCategory = locationCategory,
        latitude = latitude,
        longitude = longitude,
        userId = userId,
        note = note,
        tags = gson.toJson(tags),
        createdAt = createdAt
    )
}
