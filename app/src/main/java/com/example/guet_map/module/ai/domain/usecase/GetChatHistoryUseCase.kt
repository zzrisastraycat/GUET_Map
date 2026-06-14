package com.example.guet_map.module.ai.domain.usecase

import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.data.repository.ChatRepository
import com.example.guet_map.module.ai.domain.service.AiService
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取对话历史用例
 */
class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(sessionId: String): Flow<List<ChatMessage>> {
        return chatRepository.getMessages(sessionId)
    }
}
