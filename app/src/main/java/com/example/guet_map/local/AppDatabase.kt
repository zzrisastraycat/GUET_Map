package com.example.guet_map.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.guet_map.local.dao.GuideStepDao
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.entity.GuideStepEntity
import com.example.guet_map.local.entity.LocationEntity

@Database(
    entities = [
        GuideStepEntity::class,
        LocationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun guideStepDao(): GuideStepDao
    abstract fun locationDao(): LocationDao

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
