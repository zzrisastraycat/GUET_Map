package com.example.guet_map.module.ai.domain.usecase

import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.domain.service.AiService
import com.example.guet_map.model.Resource
import javax.inject.Inject

/**
 * 发送消息用例
 */
class SendMessageUseCase @Inject constructor(
    private val aiService: AiService
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        locationId: String? = null
    ): Resource<ChatMessage> {
        return aiService.sendMessage(sessionId, message, locationId)
    }
}
