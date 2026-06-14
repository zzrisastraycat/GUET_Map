package com.example.guet_map.module.social.data.model

/**
 * 收藏
 */
data class Favorite(
    val id: String,
    val locationId: String,
    val locationName: String,
    val locationCategory: String,
    val latitude: Double,
    val longitude: Double,
    val userId: String,
    val note: String? = null,     // 用户备注
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 收藏分类（用于筛选）
 */
enum class FavoriteCategory(val displayName: String) {
    ALL("全部"),
    FOOD("美食"),
    STUDY("学习"),
    SPORTS("运动"),
    ENTERTAINMENT("娱乐"),
    DAILY("生活"),
    OTHER("其他")
}
