package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.core.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地点 DAO（旧版兼容）
 */
@Dao
interface LocationDao {

    @Query("SELECT * FROM locations")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE category = :category")
    fun getLocationsByCategory(category: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE location_id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int
}
