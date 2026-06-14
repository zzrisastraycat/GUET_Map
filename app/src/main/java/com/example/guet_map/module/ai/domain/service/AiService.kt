package com.example.guet_map.module.ai.domain.service

import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务接口
 * 定义 AI 对话的核心能力
 */
interface AiService {

    /**
     * 发送消息并获取 AI 回复
     */
    suspend fun sendMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Resource<ChatMessage>

    /**
     * 流式发送消息（SSE）
     */
    fun sendMessageStream(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Flow<Resource<String>>  // 流式返回片段

    /**
     * 生成引导性问题
     */
    suspend fun generateGuidedQuestions(locationId: String?): Resource<List<String>>
}
