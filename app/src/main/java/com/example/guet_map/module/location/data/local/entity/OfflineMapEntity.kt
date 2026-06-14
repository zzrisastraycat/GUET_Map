package com.example.guet_map.module.location.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 离线地图缓存实体
 */
@Entity(tableName = "offline_maps")
data class OfflineMapEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val size: Long,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val localPath: String?,  // 本地存储路径
    val status: String,
    val progress: Int,
    val downloadedSize: Long,
    val lastUpdateTime: Long
)
