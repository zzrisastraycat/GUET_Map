package com.example.guet_map.module.location.data.local.dao

import androidx.room.*
import com.example.guet_map.module.location.data.local.entity.AnnouncementEntity
import kotlinx.coroutines.flow.Flow

/**
 * 公告 DAO
 */
@Dao
interface AnnouncementDao {

    @Query("SELECT * FROM announcements ORDER BY isPinned DESC, priority DESC, publishTime DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE category = :category ORDER BY isPinned DESC, priority DESC, publishTime DESC")
    fun getAnnouncementsByCategory(category: String): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun getAnnouncementById(id: String): AnnouncementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    @Query("UPDATE announcements SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    @Query("DELETE FROM announcements WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}
