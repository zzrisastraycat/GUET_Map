package com.example.guet_map.module.ai.data.model

/**
 * 地点评论
 */
data class Review(
    val id: String,
    val locationId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val rating: Float,  // 1-5 星
    val content: String,
    val images: List<String> = emptyList(),  // 评论图片 URLs
    val likes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val replyCount: Int = 0
)

/**
 * 评论回复
 */
data class ReviewReply(
    val id: String,
    val reviewId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
