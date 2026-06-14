package com.example.guet_map.module.location.data.model

/**
 * 离线地图
 */
data class OfflineMap(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val size: Long,  // 字节
    val downloadUrl: String,
    val thumbnailUrl: String? = null,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val progress: Int = 0,  // 0-100
    val downloadedSize: Long = 0,
    val lastUpdateTime: Long = 0
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    UPDATE_AVAILABLE,
    FAILED
}
