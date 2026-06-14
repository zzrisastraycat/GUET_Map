package com.example.guet_map.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.guet_map.core.entity.ContributeDraftEntity
import com.example.guet_map.core.entity.GuideStepEntity
import com.example.guet_map.core.entity.LocationEntity
import com.example.guet_map.core.entity.NotificationEntity
import com.example.guet_map.local.entity.LegacyFavoriteEntity
import com.example.guet_map.local.entity.LegacyLocationEntity
import com.example.guet_map.module.ai.data.local.entity.ChatMessageEntity
import com.example.guet_map.module.ai.data.local.entity.ReviewEntity
import com.example.guet_map.module.location.data.local.entity.AnnouncementEntity
import com.example.guet_map.module.location.data.local.entity.OfflineMapEntity
import com.example.guet_map.module.social.data.local.entity.FavoriteEntity
import com.example.guet_map.module.social.data.local.entity.PhotoAlbumEntity
import com.example.guet_map.module.social.data.local.entity.PhotoEntity
import com.example.guet_map.module.social.data.local.entity.WeatherEntity

/**
 * 应用数据库 - 统一管理所有 Entity 和 DAOs
 */
@Database(
    entities = [
        // ========== Core 模块 ==========
        LocationEntity::class,
        GuideStepEntity::class,
        NotificationEntity::class,
        ContributeDraftEntity::class,

        // ========== Legacy 模块 (旧代码兼容) ==========
        LegacyFavoriteEntity::class,
        LegacyLocationEntity::class,

        // ========== AI 模块 ==========
        ChatMessageEntity::class,
        ReviewEntity::class,

        // ========== Location 模块 ==========
        AnnouncementEntity::class,
        OfflineMapEntity::class,

        // ========== Social 模块 ==========
        WeatherEntity::class,
        FavoriteEntity::class,
        PhotoAlbumEntity::class,
        PhotoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // ========== Core DAOs ==========
    abstract fun locationDao(): com.example.guet_map.core.dao.LocationDao
    abstract fun guideStepDao(): com.example.guet_map.core.dao.GuideStepDao
    abstract fun notificationDao(): com.example.guet_map.core.dao.NotificationDao
    abstract fun contributeDraftDao(): com.example.guet_map.core.dao.ContributeDraftDao

    // ========== Legacy DAOs (旧代码兼容) ==========
    abstract fun legacyFavoriteDao(): com.example.guet_map.local.dao.LegacyFavoriteDao
    abstract fun legacyLocationDao(): com.example.guet_map.local.dao.LegacyLocationDao

    // ========== AI 模块 DAOs ==========
    abstract fun chatMessageDao(): com.example.guet_map.module.ai.data.local.dao.ChatMessageDao
    abstract fun reviewDao(): com.example.guet_map.module.ai.data.local.dao.ReviewDao

    // ========== Location 模块 DAOs ==========
    abstract fun announcementDao(): com.example.guet_map.module.location.data.local.dao.AnnouncementDao
    abstract fun offlineMapDao(): com.example.guet_map.module.location.data.local.dao.OfflineMapDao

    // ========== Social 模块 DAOs ==========
    abstract fun weatherDao(): com.example.guet_map.module.social.data.local.dao.WeatherDao
    abstract fun favoriteDao(): com.example.guet_map.module.social.data.local.dao.FavoriteDao
    abstract fun photoDao(): com.example.guet_map.module.social.data.local.dao.PhotoDao
}
