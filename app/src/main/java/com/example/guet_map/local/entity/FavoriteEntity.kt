package com.example.guet_map.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["user_id", "location_id"]
)
data class FavoriteEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "location_id")
    val locationId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
