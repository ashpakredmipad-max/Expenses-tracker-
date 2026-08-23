package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val yearMonth: String, // e.g. "2026-08" or "DEFAULT"
    val budgetInPaise: Long
)
