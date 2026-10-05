package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class IdOcrResult(
    val name: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val confidenceHint: String = "Review before saving"
)

object IdOcrRecognizer {
    suspend fun recognize(context: Context, pathOrUri: String): IdOcrResult {
        val bitmap = decodeForOcr(context, pathOrUri) ?: return IdOcrResult()

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = suspendCancellableCoroutine<String> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }.also { recognizer.close() }
                .trim()
            parse(text)
        } catch (_: Exception) {
            recognizer.close()
            IdOcrResult()
        }
    }


    private fun decodeForOcr(context: Context, pathOrUri: String): android.graphics.Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        fun open() = if (pathOrUri.startsWith("content://")) {
            context.contentResolver.openInputStream(android.net.Uri.parse(pathOrUri))
        } else {
            File(pathOrUri).inputStream()
        }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide <= 0) return null
        var sample = 1
        while (maxSide / sample > 1800) sample *= 2
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        return open()?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun parse(text: String): IdOcrResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val upper = text.uppercase()

        val type = when {
            Regex("""\b[A-Z]{5}[0-9]{4}[A-Z]\b""").containsMatchIn(upper) ||
                upper.contains("INCOME TAX") || upper.contains("PERMANENT ACCOUNT NUMBER") -> "PAN Card"
            upper.contains("AADHAAR") || upper.contains("UIDAI") ||
                Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""").containsMatchIn(text) -> "Aadhaar Card"
            upper.contains("DRIVING LICEN[CS]E") || upper.contains("DL NO") -> "Driving License"
            upper.contains("ELECTION COMMISSION") || upper.contains("EPIC") ||
                upper.contains("VOTER") -> "Voter ID"
            upper.contains("PASSPORT") -> "Passport"
            else -> ""
        }

        val pan = Regex("""\b[A-Z]{5}[0-9]{4}[A-Z]\b""").find(upper)?.value
        val aadhaar = Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""").find(text)?.value
        val candidateId = pan ?: aadhaar ?: lines.firstOrNull {
            val compact = it.replace(" ", "")
            compact.length in 8..20 && compact.any(Char::isDigit) && compact.any(Char::isLetter)
        }

        val name = lines.firstOrNull { line ->
            val u = line.uppercase()
            line.length in 3..60 &&
                line.count { it.isLetter() } >= 3 &&
                !u.contains("GOVERNMENT") &&
                !u.contains("AADHAAR") &&
                !u.contains("UIDAI") &&
                !u.contains("INCOME TAX") &&
                !u.contains("PASSPORT") &&
                !u.contains("DRIVING") &&
                !u.contains("ELECTION") &&
                !u.contains("CARD") &&
                !u.contains("INDIA") &&
                !u.any { it.isDigit() }
        }.orEmpty()

        return IdOcrResult(
            name = name,
            idType = type,
            idNumber = candidateId.orEmpty(),
            confidenceHint = if (name.isNotBlank() && candidateId != null && type.isNotBlank())
                "High-confidence pattern match — please confirm"
            else "OCR result needs confirmation"
        )
    }
}
