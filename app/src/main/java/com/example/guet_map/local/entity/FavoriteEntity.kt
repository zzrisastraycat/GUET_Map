package com.example.guet_map.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * 收藏实体（旧版兼容）
 * 表名: legacy_favorites
 * 主键: (user_id, location_id)
 */
@Entity(
    tableName = "legacy_favorites",
    primaryKeys = ["user_id", "location_id"]
)
data class LegacyFavoriteEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "location_id")
    val locationId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
