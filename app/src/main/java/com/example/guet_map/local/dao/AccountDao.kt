package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId LIMIT 1")
    fun observeAccount(userId: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE userId = :userId LIMIT 1")
    suspend fun getAccount(userId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)
}
