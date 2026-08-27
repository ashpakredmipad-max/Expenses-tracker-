package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registered_upi")
data class RegisteredUpiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val upiName: String,
    val categoryId: Long,
    val categoryName: String,
    val walletName: String,
    val createdAt: Long = System.currentTimeMillis()
)
