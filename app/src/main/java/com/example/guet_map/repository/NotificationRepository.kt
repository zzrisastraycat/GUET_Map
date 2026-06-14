package com.example.guet_map.repository

import com.example.guet_map.core.dao.NotificationDao
import com.example.guet_map.core.entity.NotificationEntity
import com.example.guet_map.model.AppNotification
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao
) {

    fun observeNotifications(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    suspend fun refresh() {
        try {
            val remote = apiService.getNotifications()
            notificationDao.deleteAll()
            notificationDao.insertAll(remote.map { it.toEntity() })
        } catch (_: Exception) {
            // 使用缓存
        }
    }

    suspend fun markAllRead() {
        notificationDao.markAllRead()
    }

    suspend fun markRead(id: Long) {
        notificationDao.markRead(id)
    }

    private fun AppNotification.toEntity() = NotificationEntity(
        id = id,
        type = type,
        title = title,
        body = body,
        locationId = locationId,
        isRead = isRead,
        createdAt = createdAt
    )

    private fun NotificationEntity.toDomain() = AppNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        locationId = locationId,
        isRead = isRead,
        createdAt = createdAt
    )
}
