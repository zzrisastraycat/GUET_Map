package com.example.guet_map.module.location.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Location 模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    // 如需提供特定依赖，可在此添加 @Provides 方法
    // 大部分依赖通过 @Inject 构造函数自动注入
}
