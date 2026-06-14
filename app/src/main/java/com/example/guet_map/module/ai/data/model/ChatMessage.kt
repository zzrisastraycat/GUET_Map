package com.example.guet_map.module.ai.data.model

/**
 * AI 对话消息
 */
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val locationId: String? = null  // 关联的地点（如果有）
)

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}
