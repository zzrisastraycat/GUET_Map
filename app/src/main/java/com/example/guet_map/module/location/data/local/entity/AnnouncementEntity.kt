package com.example.guet_map.module.location.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 公告缓存实体
 */
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val priority: Int,
    val publishTime: Long,
    val author: String,
    val images: String,  // JSON 数组
    val attachments: String,  // JSON 数组
    val viewCount: Int,
    val isPinned: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)
