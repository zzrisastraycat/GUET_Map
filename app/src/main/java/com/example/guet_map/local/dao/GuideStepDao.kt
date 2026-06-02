package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.GuideStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideStepDao {

    @Query("SELECT * FROM guide_steps WHERE location_id = :locationId ORDER BY step_number ASC")
    fun getGuideStepsByLocation(locationId: String): Flow<List<GuideStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<GuideStepEntity>)

    @Query("DELETE FROM guide_steps WHERE location_id = :locationId")
    suspend fun deleteByLocation(locationId: String)

    @Query("SELECT COUNT(*) FROM guide_steps WHERE location_id = :locationId")
    suspend fun countByLocation(locationId: String): Int
}
