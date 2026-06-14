package com.example.guet_map.module.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户评论
 */
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    val locationId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val rating: Float,
    val content: String,
    val images: String,  // JSON 数组
    val likes: Int,
    val createdAt: Long,
    val replyCount: Int
)
