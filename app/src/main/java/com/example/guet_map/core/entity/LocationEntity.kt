package com.example.guet_map.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 地点实体
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "location_id")
    val locationId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "rating")
    val rating: Float = 0f,

    @ColumnInfo(name = "opening_hours")
    val openingHours: String = "",

    @ColumnInfo(name = "image_url")
    val imageUrl: String = "",

    @ColumnInfo(name = "has_guide")
    val hasGuide: Boolean = false
)
