package com.example.guet_map.module.location.data.repository

import com.example.guet_map.module.location.data.local.dao.AnnouncementDao
import com.example.guet_map.module.location.data.local.entity.AnnouncementEntity
import com.example.guet_map.module.location.data.model.Announcement
import com.example.guet_map.module.location.data.model.AnnouncementCategory
import com.example.guet_map.model.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 公告仓库
 */
@Singleton
class AnnouncementRepository @Inject constructor(
    private val announcementDao: AnnouncementDao,
    private val gson: Gson
) {

    fun getAllAnnouncements(): Flow<List<Announcement>> {
        return announcementDao.getAllAnnouncements().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAnnouncementsByCategory(category: AnnouncementCategory): Flow<List<Announcement>> {
        return announcementDao.getAnnouncementsByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAnnouncementById(id: String): Announcement? {
        return announcementDao.getAnnouncementById(id)?.toDomain()
    }

    suspend fun refreshAnnouncements(): Resource<List<Announcement>> {
        // TODO: 调用真实 API 获取公告
        // 目前返回模拟数据
        return Resource.Success(emptyList())
    }

    suspend fun markAsViewed(id: String) {
        announcementDao.incrementViewCount(id)
    }

    suspend fun cacheAnnouncements(announcements: List<Announcement>) {
        announcementDao.insertAnnouncements(announcements.map { it.toEntity() })
    }

    private fun AnnouncementEntity.toDomain() = Announcement(
        id = id,
        title = title,
        content = content,
        category = AnnouncementCategory.valueOf(category),
        priority = priority,
        publishTime = publishTime,
        author = author,
        images = gson.fromJson(images, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        attachments = gson.fromJson(attachments, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        viewCount = viewCount,
        isPinned = isPinned
    )

    private fun Announcement.toEntity() = AnnouncementEntity(
        id = id,
        title = title,
        content = content,
        category = category.name,
        priority = priority,
        publishTime = publishTime,
        author = author,
        images = gson.toJson(images),
        attachments = gson.toJson(attachments),
        viewCount = viewCount,
        isPinned = isPinned
    )
}
