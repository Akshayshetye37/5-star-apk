package com.example.util

import android.content.Context

object AppPreferences {
    private const val FILE = "hotel_app_preferences"
    private const val OCR = "ocr_enabled"
    private const val COMPACT = "compact_ui"
    private const val DARK = "dark_mode"
    private const val INVOICE_STYLE = "invoice_style"

    private fun p(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun ocrEnabled(context: Context): Boolean = p(context).getBoolean(OCR, true)
    fun setOcrEnabled(context: Context, value: Boolean) { p(context).edit().putBoolean(OCR, value).apply() }

    fun compactUi(context: Context): Boolean = p(context).getBoolean(COMPACT, false)
    fun setCompactUi(context: Context, value: Boolean) { p(context).edit().putBoolean(COMPACT, value).apply() }

    fun darkMode(context: Context): Boolean = p(context).getBoolean(DARK, false)
    fun setDarkMode(context: Context, value: Boolean) { p(context).edit().putBoolean(DARK, value).apply() }

    fun invoiceStyle(context: Context): String = p(context).getString(INVOICE_STYLE, "CLASSIC") ?: "CLASSIC"
    fun setInvoiceStyle(context: Context, value: String) { p(context).edit().putString(INVOICE_STYLE, value).apply() }
}
