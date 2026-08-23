package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    val AVAILABLE_ICONS = listOf(
        "Restaurant" to Icons.Default.Restaurant,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "ReceiptLong" to Icons.Default.ReceiptLong,
        "Bolt" to Icons.Default.Bolt,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "Wifi" to Icons.Default.Wifi,
        "Home" to Icons.Default.Home,
        "MedicalServices" to Icons.Default.MedicalServices,
        "Movie" to Icons.Default.Movie,
        "School" to Icons.Default.School,
        "Flight" to Icons.Default.Flight,
        "Payments" to Icons.Default.Payments,
        "Storefront" to Icons.Default.Storefront,
        "LaptopMac" to Icons.Default.LaptopMac,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "TrendingUp" to Icons.Default.TrendingUp,
        "Savings" to Icons.Default.Savings,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Coffee" to Icons.Default.Coffee,
        "Pets" to Icons.Default.Pets,
        "Work" to Icons.Default.Work,
        "AccountBalance" to Icons.Default.AccountBalance,
        "Build" to Icons.Default.Build,
        "DirectionsBus" to Icons.Default.DirectionsBus,
        "Category" to Icons.Default.Category
    )

    val PRESET_COLORS = listOf(
        "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
        "#5C6BC0", "#42A5F5", "#29B6F6", "#26C6DA",
        "#26A69A", "#4CAF50", "#66BB6A", "#9CCC65",
        "#FBC02D", "#FFA726", "#FF7043", "#8D6E63",
        "#78909C", "#00897B", "#1E88E5", "#F57F17"
    )

    fun getIcon(iconName: String): ImageVector {
        return AVAILABLE_ICONS.firstOrNull { it.first.equals(iconName, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF006C50)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorLong)
            } else if (cleanHex.length == 8) {
                Color(colorLong)
            } else {
                fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
