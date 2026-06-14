package com.example.guet_map.core.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 投稿草稿实体
 */
@Entity(tableName = "contribute_drafts")
data class ContributeDraftEntity(
    @PrimaryKey
    val id: Int = 1,
    val locationId: String = "",
    val locationName: String = "",
    val stepsJson: String = ""
)
