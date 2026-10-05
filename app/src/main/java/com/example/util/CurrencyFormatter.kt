package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "₹"): String {
        val formatter = DecimalFormat("#,##,##0.00")
        return "$symbol ${formatter.format(amount)}"
    }

    fun formatWhole(amount: Double, symbol: String = "₹"): String {
        val formatter = DecimalFormat("#,##,##0")
        return "$symbol ${formatter.format(amount)}"
    }

    fun parseAmount(text: String): Double {
        val cleaned = text.replace(",", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
