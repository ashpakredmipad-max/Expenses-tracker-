package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("type"),
        Index("categoryId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amountInPaise: Long, // e.g. ₹500.00 is stored as 50000 paise to prevent float rounding errors
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val date: Long, // Epoch timestamp in milliseconds
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
