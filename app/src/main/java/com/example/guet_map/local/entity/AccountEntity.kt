package com.example.guet_map.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val userId: String,
    val nickname: String,
    val points: Int,
    val contributionCount: Int,
    val token: String,
    val passwordHash: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
