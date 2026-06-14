package com.example.guet_map.module.ai.di

import com.example.guet_map.module.ai.data.local.dao.ChatMessageDao
import com.example.guet_map.module.ai.data.local.dao.ReviewDao
import com.example.guet_map.module.ai.domain.service.AiService
import com.example.guet_map.module.ai.domain.service.AiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AI 模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindAiService(impl: AiServiceImpl): AiService

    companion object {
        // 如需提供特定依赖，可在此添加 @Provides 方法
    }
}
