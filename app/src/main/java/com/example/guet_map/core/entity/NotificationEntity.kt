package com.example.guet_map.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 通知实体
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "location_id")
    val locationId: String? = null,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String = ""
)
