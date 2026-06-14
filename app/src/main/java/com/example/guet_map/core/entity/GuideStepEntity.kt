package com.example.guet_map.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 图文指引步骤实体
 */
@Entity(tableName = "guide_steps")
data class GuideStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "location_id")
    val locationId: String,

    @ColumnInfo(name = "step_number")
    val stepNumber: Int,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "image_url")
    val imageUrl: String
)
