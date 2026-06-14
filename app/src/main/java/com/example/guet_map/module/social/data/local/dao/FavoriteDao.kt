package com.example.guet_map.module.social.data.local.dao

import androidx.room.*
import com.example.guet_map.module.social.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * 收藏 DAO
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFavoritesByUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND locationCategory = :category ORDER BY createdAt DESC")
    fun getFavoritesByCategory(userId: String, category: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND locationId = :locationId")
    suspend fun getFavorite(userId: String, locationId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationId = :locationId)")
    suspend fun isFavorite(userId: String, locationId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationId = :locationId)")
    fun observeIsFavorite(userId: String, locationId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND locationId = :locationId")
    suspend fun deleteFavoriteByLocation(userId: String, locationId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId")
    suspend fun getFavoriteCount(userId: String): Int
}
