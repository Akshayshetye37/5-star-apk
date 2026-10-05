package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Customer
import com.example.data.model.HotelSettings
import com.example.data.model.Payment
import java.io.File
import java.io.FileOutputStream

/**
 * Multi-guest room/booking PDF. Pass a single guest to [guests] for a
 * single-person PDF, or all guests for a complete room PDF.
 */
object GuestRoomPdfGenerator {
    fun generate(
        context: Context,
        settings: HotelSettings,
        booking: Booking,
        customer: Customer?,
        payments: List<Payment>,
        reservationId: String = "",
        guests: List<BookingGuest> = emptyList()
    ): File {
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 59, 95)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
        }
        val margin = 36f
        val pageWidth = 595f
        val pageHeight = 842f
        var pageNumber = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y: Float = 0f

        val guestList = if (guests.isNotEmpty()) guests else listOf(
            BookingGuest(
                guestId = "legacy",
                bookingId = booking.bookingId,
                sequence = 1,
                name = booking.customerName,
                idType = booking.idType,
                idNumber = booking.idNumber,
                phone = booking.contactNumber.ifBlank { customer?.phone.orEmpty() },
                whatsapp = booking.whatsappNumber.ifBlank { customer?.whatsapp.orEmpty() },
                idPhotoUri = booking.idPhotoUri.ifBlank { customer?.idPhotoUri.orEmpty() }
            )
        )

        fun loadBitmap(path: String) = try {
            when {
                path.startsWith("content://") || path.startsWith("android.resource://") ->
                    context.contentResolver.openInputStream(Uri.parse(path))?.use { BitmapFactory.decodeStream(it) }
                path.isNotBlank() -> BitmapFactory.decodeFile(path)
                else -> null
            }
        } catch (_: Exception) { null }

        fun startPage(headerTitle: String) {
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, pageWidth, 88f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 59, 95)
            })
            val logo = loadBitmap(settings.logoUri)
            logo?.let {
                val scale = minOf(48f / it.width, 48f / it.height)
                canvas.drawBitmap(it, null, android.graphics.RectF(margin, 10f, margin + it.width * scale, 10f + it.height * scale), null)
            }
            val headerX = if (logo != null) margin + 62f else margin
            title.textSize = 18f
            canvas.drawText(settings.hotelName.ifBlank { "HOTEL" }.uppercase(), headerX, 32f, title)
            title.textSize = 9f
            title.color = Color.rgb(251, 191, 36)
            canvas.drawText(settings.slogan, headerX, 48f, title)
            title.color = Color.WHITE
            title.textSize = 13f
            canvas.drawText(headerTitle, pageWidth - 205f, 40f, title)
            small.color = Color.rgb(226, 232, 240)
            canvas.drawText(settings.address, headerX, 65f, small)
            canvas.drawText("Phone: ${settings.phone}", headerX, 78f, small)
            y = 112f
        }

        fun finishPage() {
            doc.finishPage(page)
        }

        fun section(name: String) {
            heading.textSize = 11f
            canvas.drawText(name, margin, y, heading)
            y += 18f
        }

        fun row(label: String, value: String) {
            text.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(label, margin, y, text)
            text.typeface = Typeface.DEFAULT
            canvas.drawText(value.ifBlank { "—" }.take(72), 170f, y, text)
            y += 16f
        }

        startPage(if (guestList.size == 1) "PERSON DETAILS" else "ROOM DETAILS")

        section("ROOM & BOOKING")
        row("Room Number", booking.roomNumber)
        row("Room Type", booking.roomType)
        row("Booking ID", booking.bookingId)
        if (reservationId.isNotBlank()) row("Reservation ID", reservationId)
        row("Booking Status", booking.status)
        row("Check-in", "${booking.checkInDate} ${booking.checkInTime}")
        row("Check-out", "${booking.checkOutDate} ${booking.checkOutTime}")
        row("Number of Guests", guestList.size.toString())
        y += 8f

        section(if (guestList.size == 1) "GUEST DETAIL" else "ALL GUESTS IN ROOM")
        guestList.forEach { guest ->
            val photo = loadBitmap(guest.idPhotoUri)
            val blockHeight = if (photo != null) 86f else 58f
            if (y + blockHeight > pageHeight - 50f) {
                finishPage()
                startPage("ROOM GUESTS — CONTINUED")
                section("GUESTS IN ROOM")
            }
            text.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Guest ${guest.sequence}: ${guest.name.ifBlank { "—" }}", margin, y, text)
            text.typeface = Typeface.DEFAULT
            canvas.drawText("Contact: ${guest.phone.ifBlank { "—" }}", margin, y + 15f, text)
            canvas.drawText("WhatsApp: ${guest.whatsapp.ifBlank { "—" }}", margin, y + 30f, text)
            canvas.drawText("ID: ${guest.idType}: ${guest.idNumber.ifBlank { "—" }}", margin, y + 45f, text)
            if (guest.vehicleNumbers.isNotBlank()) {
                canvas.drawText("Vehicle(s): ${guest.vehicleNumbers.replace("\n", ", ")}", margin, y + 60f, small)
            }
            photo?.let {
                val scale = minOf(90f / it.width, 65f / it.height)
                canvas.drawBitmap(
                    it, null,
                    android.graphics.RectF(pageWidth - margin - 90f, y - 10f, pageWidth - margin, y - 10f + it.height * scale),
                    null
                )
            }
            y += blockHeight
            canvas.drawLine(margin, y, pageWidth - margin, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            })
            y += 8f
        }

        // Charges and payment history always start on a fresh page.
        finishPage()
        startPage("BILLING & PAYMENTS")
        section("CHARGES")
        row("Room Charges", CurrencyFormatter.format(booking.roomCharges, settings.currencySymbol))
        val food = booking.mineralWater + booking.ghavaneChatney + booking.tea + booking.kandePohe
        row("Food Charges", CurrencyFormatter.format(food, settings.currencySymbol))
        row("Other Charges", CurrencyFormatter.format(booking.otherCharges, settings.currencySymbol))
        row("Discount", CurrencyFormatter.format(booking.discount, settings.currencySymbol))
        row("Grand Total", CurrencyFormatter.format(booking.grandTotal, settings.currencySymbol))
        row("Cash / Advance", CurrencyFormatter.format(booking.advance, settings.currencySymbol))
        row("Total Paid", CurrencyFormatter.format(booking.paid, settings.currencySymbol))
        row("Pending Balance", CurrencyFormatter.format(booking.pending, settings.currencySymbol))
        y += 10f
        section("PAYMENT HISTORY")
        payments.forEach { payment ->
            if (y > pageHeight - 45f) {
                finishPage()
                startPage("PAYMENT HISTORY — CONTINUED")
                section("PAYMENT HISTORY")
            }
            canvas.drawText(
                "${DateUtils.formatDisplayDateTime(payment.date)} • ${payment.paymentMethod} • ${CurrencyFormatter.format(payment.amount, settings.currencySymbol)}",
                margin, y, text
            )
            y += 16f
            if (payment.referenceNumber.isNotBlank()) {
                canvas.drawText("Ref: ${payment.referenceNumber}", margin + 12f, y, small)
                y += 14f
            }
        }
        if (booking.pending > 0 && settings.upiId.isNotBlank()) {
            y += 8f
            if (y > pageHeight - 150f) {
                finishPage()
                startPage("PAYMENT QR")
            }
            section("PAYMENT REQUEST")
            val upiUrl = QrCodeGenerator.buildUpiUrl(
                settings.upiId,
                settings.upiPayeeName.ifBlank { settings.hotelName },
                booking.pending
            )
            QrCodeGenerator.generateBitmap(upiUrl, 120)?.let { qr ->
                canvas.drawBitmap(
                    qr, null,
                    android.graphics.RectF(margin, y, margin + 120f, y + 120f),
                    null
                )
                text.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("Outstanding balance: ${CurrencyFormatter.format(booking.pending, settings.currencySymbol)}", margin + 135f, y + 28f, text)
                text.typeface = Typeface.DEFAULT
                canvas.drawText("Reason: outstanding hotel bill balance for Room ${booking.roomNumber}.", margin + 135f, y + 48f, small)
                canvas.drawText("UPI: ${settings.upiId}", margin + 135f, y + 64f, small)
                y += 130f
            }
        }

        if (booking.notes.isNotBlank()) {
            y += 8f
            section("NOTES")
            booking.notes.split("\n").take(8).forEach { note ->
                if (y > pageHeight - 40f) {
                    finishPage()
                    startPage("NOTES — CONTINUED")
                    section("NOTES")
                }
                canvas.drawText(note.take(100), margin, y, text)
                y += 14f
            }
        }
        finishPage()

        val dir = File(context.filesDir, "guest_pdfs").apply { mkdirs() }
        val suffix = if (guestList.size == 1) "person" else "room"
        val file = File(dir, "guest_${booking.bookingId}_${booking.roomNumber}_$suffix.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Guest / Room PDF"))
    }

    fun open(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    fun print(context: Context, file: File) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print("Guest_${file.nameWithoutExtension}", PdfPrintAdapter(context, file), PrintAttributes.Builder().build())
    }

    private class PdfPrintAdapter(
        private val context: Context,
        private val file: File
    ) : android.print.PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback?, extras: android.os.Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            callback?.onLayoutFinished(
                android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: android.os.ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            java.io.FileInputStream(file).use { input ->
                android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        }
    }
}
