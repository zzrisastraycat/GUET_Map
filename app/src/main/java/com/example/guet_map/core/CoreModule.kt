package com.example.guet_map.core

/**
 * Core 核心共享模块
 *
 * 包含各模块共用的核心功能：
 * - 地图相关模型和服务
 * - 用户信息
 * - 通用扩展
 */
object CoreModule {
    const val PACKAGE = "com.example.guet_map.core"

    object Path {
        const val MAP = "$PACKAGE.map"
        const val USER = "$PACKAGE.user"
        const val DATABASE = "$PACKAGE.database"
        const val NETWORK = "$PACKAGE.network"
    }
}
