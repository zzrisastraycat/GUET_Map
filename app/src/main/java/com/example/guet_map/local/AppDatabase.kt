package com.example.guet_map.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.guet_map.local.dao.ContributeDraftDao
import com.example.guet_map.local.dao.FavoriteDao
import com.example.guet_map.local.dao.GuideStepDao
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.dao.NotificationDao
import com.example.guet_map.local.entity.ContributeDraftEntity
import com.example.guet_map.local.entity.FavoriteEntity
import com.example.guet_map.local.entity.GuideStepEntity
import com.example.guet_map.local.entity.LocationEntity
import com.example.guet_map.local.entity.NotificationEntity

@Database(
    entities = [
        GuideStepEntity::class,
        LocationEntity::class,
        FavoriteEntity::class,
        NotificationEntity::class,
        ContributeDraftEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun guideStepDao(): GuideStepDao
    abstract fun locationDao(): LocationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun notificationDao(): NotificationDao
    abstract fun contributeDraftDao(): ContributeDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guet_map.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
