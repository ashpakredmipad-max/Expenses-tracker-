package com.example.data.model

enum class TransactionType(val title: String) {
    EXPENSE("Expense"),
    INCOME("Income");

    companion object {
        fun fromString(value: String): TransactionType {
            return if (value.equals("INCOME", ignoreCase = true)) INCOME else EXPENSE
        }
    }
}
