package com.example.guet_map.core.map.model

/**
 * 地点信息（核心共享模型）
 */
data class Location(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val images: List<String> = emptyList(),
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val tags: List<String> = emptyList(),
    val facilities: List<String> = emptyList(),
    val openingHours: String? = null,
    val contactPhone: String? = null,
    val isPinned: Boolean = false
)

/**
 * 地点分类
 */
object LocationCategory {
    const val FOOD = "美食"
    const val STUDY = "学习"
    const val SPORTS = "运动"
    const val ENTERTAINMENT = "娱乐"
    const val DAILY = "生活"
    const val ADMIN = "行政"
    const val DORMITORY = "宿舍"
    const val OTHER = "其他"

    val ALL = listOf(FOOD, STUDY, SPORTS, ENTERTAINMENT, DAILY, ADMIN, DORMITORY, OTHER)
}
