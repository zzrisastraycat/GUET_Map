package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.LegacyFavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * 收藏 DAO（旧版兼容）
 * 表名: legacy_favorites
 */
@Dao
interface LegacyFavoriteDao {

    @Query("SELECT * FROM legacy_favorites WHERE user_id = :userId ORDER BY added_at DESC")
    fun observeAll(userId: String): Flow<List<LegacyFavoriteEntity>>

    @Query("SELECT location_id FROM legacy_favorites WHERE user_id = :userId")
    fun observeFavoriteIds(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM legacy_favorites WHERE user_id = :userId AND location_id = :locationId")
    suspend fun isFavorite(userId: String, locationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LegacyFavoriteEntity)

    @Query("DELETE FROM legacy_favorites WHERE user_id = :userId AND location_id = :locationId")
    suspend fun delete(userId: String, locationId: String)

    @Query("DELETE FROM legacy_favorites WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
