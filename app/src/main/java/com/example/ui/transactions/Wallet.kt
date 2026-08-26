package com.example.ui.transactions

import androidx.compose.runtime.Immutable

@Immutable
data class Wallet(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0
)
