package com.example.guet_map.module.social.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 收藏实体
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val locationId: String,
    val locationName: String,
    val locationCategory: String,
    val latitude: Double,
    val longitude: Double,
    val userId: String,
    val note: String?,
    val tags: String,           // JSON 数组
    val createdAt: Long
)
