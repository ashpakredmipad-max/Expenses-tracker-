package com.example.ui.model

import com.example.data.local.database.entity.CategoryEntity

data class CategorySpendItem(
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val totalAmountInPaise: Long,
    val percentage: Float, // 0.0 to 100.0
    val transactionCount: Int
)

data class MonthSummary(
    val totalIncomeInPaise: Long = 0,
    val totalExpenseInPaise: Long = 0,
    val balanceInPaise: Long = 0,
    val todayExpenseInPaise: Long = 0,
    val overallBalanceInPaise: Long = 0
)
