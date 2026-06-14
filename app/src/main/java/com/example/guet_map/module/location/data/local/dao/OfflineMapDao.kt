package com.example.guet_map.module.location.data.local.dao

import androidx.room.*
import com.example.guet_map.module.location.data.local.entity.OfflineMapEntity
import kotlinx.coroutines.flow.Flow

/**
 * 离线地图 DAO
 */
@Dao
interface OfflineMapDao {

    @Query("SELECT * FROM offline_maps ORDER BY name ASC")
    fun getAllOfflineMaps(): Flow<List<OfflineMapEntity>>

    @Query("SELECT * FROM offline_maps WHERE status = 'DOWNLOADED'")
    fun getDownloadedMaps(): Flow<List<OfflineMapEntity>>

    @Query("SELECT * FROM offline_maps WHERE id = :id")
    suspend fun getOfflineMapById(id: String): OfflineMapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineMap(map: OfflineMapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineMaps(maps: List<OfflineMapEntity>)

    @Update
    suspend fun updateOfflineMap(map: OfflineMapEntity)

    @Query("UPDATE offline_maps SET status = :status, progress = :progress, downloadedSize = :downloadedSize WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, status: String, progress: Int, downloadedSize: Long)

    @Query("DELETE FROM offline_maps WHERE id = :id")
    suspend fun deleteOfflineMap(id: String)
}
