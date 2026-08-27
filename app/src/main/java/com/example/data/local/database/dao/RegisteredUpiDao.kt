package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.database.entity.RegisteredUpiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisteredUpiDao {
    @Query("SELECT * FROM registered_upi ORDER BY upiName COLLATE NOCASE")
    fun getAll(): Flow<List<RegisteredUpiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RegisteredUpiEntity): Long

    @Query("SELECT * FROM registered_upi WHERE upiName = :upiName LIMIT 1")
    suspend fun findByName(upiName: String): RegisteredUpiEntity?

    @Query("DELETE FROM registered_upi WHERE upiName = :upiName")
    suspend fun deleteByName(upiName: String)
}
