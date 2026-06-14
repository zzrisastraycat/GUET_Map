package com.example.guet_map.module.location.data.model

/**
 * 校园公告
 */
data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val category: AnnouncementCategory,
    val priority: Int = 0,  // 0-9，越大越重要
    val publishTime: Long,
    val author: String,
    val images: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val viewCount: Int = 0,
    val isPinned: Boolean = false
)

enum class AnnouncementCategory(val displayName: String) {
    ACADEMIC("学术通知"),
    ACTIVITY("活动公告"),
    SYSTEM("系统通知"),
    MAINTENANCE("维护公告"),
    EMERGENCY("紧急通知"),
    OTHER("其他")
}
