package com.example.guet_map.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.core.entity.ContributeDraftEntity

@Dao
interface ContributeDraftDao {

    @Query("SELECT * FROM contribute_drafts WHERE id = 1 LIMIT 1")
    suspend fun getDraft(): ContributeDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(draft: ContributeDraftEntity)

    @Query("DELETE FROM contribute_drafts WHERE id = 1")
    suspend fun clear()
}
