package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {
    const val CURRENCY_SYMBOL = "₹"

    /**
     * Converts paise (Long) to formatted Indian Rupee string, e.g.,
     * 2550000 -> "₹25,500.00"
     * 10000000 -> "₹1,00,000.00"
     */
    fun formatPaise(paise: Long, showDecimals: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val formattedInteger = formatIndianNumber(rupees)
        val prefix = if (isNegative) "-$CURRENCY_SYMBOL" else CURRENCY_SYMBOL

        return if (showDecimals) {
            val paiseStr = String.format(Locale.US, "%02d", remainingPaise)
            "$prefix$formattedInteger.$paiseStr"
        } else {
            "$prefix$formattedInteger"
        }
    }

    /**
     * Formats integer rupees according to Indian numbering system (e.g. 1,00,000)
     */
    fun formatIndianNumber(number: Long): String {
        if (number < 1000) return number.toString()

        val numStr = number.toString()
        val len = numStr.length

        // Last 3 digits
        val lastThree = numStr.substring(len - 3)
        var remaining = numStr.substring(0, len - 3)

        val parts = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            if (remaining.length <= 2) {
                parts.add(0, remaining)
                break
            } else {
                val part = remaining.substring(remaining.length - 2)
                parts.add(0, part)
                remaining = remaining.substring(0, remaining.length - 2)
            }
        }
        parts.add(lastThree)
        return parts.joinToString(",")
    }

    /**
     * Parses a user input string (e.g. "1250", "1250.50", "₹ 1,250.50") into paise (Long).
     * Returns null if invalid or <= 0.
     */
    fun parseAmountToPaise(input: String): Long? {
        val sanitized = input.replace(CURRENCY_SYMBOL, "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (sanitized.isEmpty()) return null

        return try {
            val value = sanitized.toDouble()
            if (value <= 0) return null
            val paise = (value * 100).toLong()
            if (paise <= 0) null else paise
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts paise to decimal string for text input editing (e.g., 2550000 -> "25500.00" or "25500")
     */
    fun paiseToInputString(paise: Long): String {
        val rupees = paise / 100
        val remainingPaise = paise % 100
        return if (remainingPaise == 0L) {
            rupees.toString()
        } else {
            String.format(Locale.US, "%d.%02d", rupees, remainingPaise)
        }
    }
}
