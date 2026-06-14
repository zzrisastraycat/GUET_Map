package com.example.guet_map.module.social.data.model

/**
 * 照片相册
 */
data class PhotoAlbum(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val photoCount: Int = 0,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 照片
 */
data class Photo(
    val id: String,
    val albumId: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val width: Int,
    val height: Int,
    val size: Long,        // 字节
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val capturedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
