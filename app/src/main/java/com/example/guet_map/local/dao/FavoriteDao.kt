package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE user_id = :userId ORDER BY added_at DESC")
    fun observeAll(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT location_id FROM favorites WHERE user_id = :userId")
    fun observeFavoriteIds(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorites WHERE user_id = :userId AND location_id = :locationId")
    suspend fun isFavorite(userId: String, locationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE user_id = :userId AND location_id = :locationId")
    suspend fun delete(userId: String, locationId: String)

    @Query("DELETE FROM favorites WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
