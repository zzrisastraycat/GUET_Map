package com.example.guet_map.module.social.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 照片相册实体
 */
@Entity(tableName = "photo_albums")
data class PhotoAlbumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val coverUrl: String?,
    val photoCount: Int,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 照片实体
 */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    val albumId: String,
    val url: String,
    val thumbnailUrl: String?,
    val width: Int,
    val height: Int,
    val size: Long,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val capturedAt: Long?,
    val createdAt: Long
)
