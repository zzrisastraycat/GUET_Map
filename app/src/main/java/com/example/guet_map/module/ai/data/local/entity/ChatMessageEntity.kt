package com.example.guet_map.module.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 对话历史记录
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val role: String,  // USER / ASSISTANT / SYSTEM
    val content: String,
    val timestamp: Long,
    val locationId: String? = null,
    val sessionId: String  // 对话会话 ID
)
