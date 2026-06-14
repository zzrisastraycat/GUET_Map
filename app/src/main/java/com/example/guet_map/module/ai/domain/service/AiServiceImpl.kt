package com.example.guet_map.module.ai.domain.service

import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.data.model.ChatRole
import com.example.guet_map.module.ai.data.repository.ChatRepository
import com.example.guet_map.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 服务实现（Mock 版本）
 * 后续可替换为真实的 GPT API 调用
 */
@Singleton
class AiServiceImpl @Inject constructor(
    private val chatRepository: ChatRepository
) : AiService {

    override suspend fun sendMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String?
    ): Resource<ChatMessage> {
        return try {
            // 保存用户消息
            chatRepository.saveMessage(sessionId, ChatRole.USER, userMessage)

            // 模拟 AI 思考延迟
            delay(800)

            // 生成回复（后续替换为真实 API）
            val response = generateMockResponse(userMessage, locationContext)

            // 保存 AI 回复
            val assistantMessage = chatRepository.saveMessage(
                sessionId = sessionId,
                role = ChatRole.ASSISTANT,
                content = response,
                locationId = locationContext
            )

            Resource.Success(assistantMessage)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "AI 服务异常")
        }
    }

    override fun sendMessageStream(
        sessionId: String,
        userMessage: String,
        locationContext: String?
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading)

        try {
            // 保存用户消息
            chatRepository.saveMessage(sessionId, ChatRole.USER, userMessage)

            // 模拟流式响应
            val response = generateMockResponse(userMessage, locationContext)
            val words = response.split("")

            for (word in words) {
                delay(50)
                emit(Resource.Success(word))
            }

            // 保存完整回复
            chatRepository.saveMessage(
                sessionId = sessionId,
                role = ChatRole.ASSISTANT,
                content = response,
                locationId = locationContext
            )
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "AI 服务异常"))
        }
    }

    override suspend fun generateGuidedQuestions(locationId: String?): Resource<List<String>> {
        return Resource.Success(
            listOf(
                "这个地点怎么走？",
                "附近有什么好吃的？",
                "开放时间是什么时候？",
                "有没有停车的地方？"
            )
        )
    }

    private fun generateMockResponse(message: String, locationContext: String?): String {
        return when {
            message.contains("怎么走") || message.contains("路线") ->
                "从您的当前位置出发，沿着主路向东走约200米，在第二个路口左转即可到达。"

            message.contains("开放") || message.contains("时间") ->
                "该地点的开放时间是：周一至周五 8:00-18:00，周末 9:00-17:00。"

            message.contains("好吃") || message.contains("美食") ->
                "附近有很多美食选择，推荐您尝试学校食堂二楼的麻辣香锅，或者后门的小吃一条街。"

            locationContext != null ->
                "关于这个地点：$locationContext。如果您有其他问题，欢迎继续问我！"

            else ->
                "您好！我是您的校园导航助手。我可以帮您：\n" +
                "• 解答地点相关问题\n" +
                "• 提供路线指引\n" +
                "• 介绍校园设施\n\n" +
                "请问有什么可以帮到您？"
        }
    }
}
