package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap
import java.util.Locale

/**
 * Reliable offline QR generator using ZXing.
 * Generates standards-compliant QR codes that can be scanned by
 * Google Pay, PhonePe, Paytm, BHIM and other UPI applications.
 */
object QrCodeGenerator {
    fun generateBitmap(content: String, size: Int = 512): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
        }
        val matrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun buildUpiUrl(
        upiId: String,
        payeeName: String,
        amount: Double? = null,
        transactionNote: String = "Hotel Room Bill"
    ): String {
        val cleanUpi = java.net.URLEncoder.encode(upiId.trim(), "UTF-8")
        val cleanName = java.net.URLEncoder.encode(payeeName.trim(), "UTF-8")
        val cleanNote = java.net.URLEncoder.encode(transactionNote.trim(), "UTF-8")
        val builder = StringBuilder(
            String.format(
                Locale.US,
                "upi://pay?pa=%s&pn=%s&cu=INR&tn=%s",
                cleanUpi, cleanName, cleanNote
            )
        )
        if (amount != null && amount > 0) {
            builder.append(String.format(Locale.US, "&am=%.2f", amount))
        }
        return builder.toString()
    }
}
