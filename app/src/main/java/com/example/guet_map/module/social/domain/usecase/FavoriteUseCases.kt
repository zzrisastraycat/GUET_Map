package com.example.guet_map.module.social.domain.usecase

import com.example.guet_map.module.social.data.model.Favorite
import com.example.guet_map.module.social.data.model.FavoriteCategory
import com.example.guet_map.module.social.data.repository.FavoriteRepository
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取收藏列表用例
 */
class GetFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(userId: String): Flow<List<Favorite>> {
        return favoriteRepository.getFavorites(userId)
    }

    fun byCategory(userId: String, category: FavoriteCategory): Flow<List<Favorite>> {
        return favoriteRepository.getFavoritesByCategory(userId, category)
    }
}

/**
 * 添加收藏用例
 */
class AddFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(
        userId: String,
        locationId: String,
        locationName: String,
        locationCategory: String,
        latitude: Double,
        longitude: Double,
        note: String? = null,
        tags: List<String> = emptyList()
    ): Resource<Favorite> {
        return favoriteRepository.addFavorite(
            userId = userId,
            locationId = locationId,
            locationName = locationName,
            locationCategory = locationCategory,
            latitude = latitude,
            longitude = longitude,
            note = note,
            tags = tags
        )
    }
}

/**
 * 取消收藏用例
 */
class RemoveFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(userId: String, locationId: String): Resource<Unit> {
        return favoriteRepository.removeFavorite(userId, locationId)
    }
}

/**
 * 检查是否已收藏用例
 */
class IsFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(userId: String, locationId: String): Boolean {
        return favoriteRepository.isFavorite(userId, locationId)
    }

    fun observe(userId: String, locationId: String): Flow<Boolean> {
        return favoriteRepository.observeIsFavorite(userId, locationId)
    }
}
