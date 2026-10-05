package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.HotelSettings
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceGenerator {

    /**
     * Generates a professional PDF invoice using Android SDK PdfDocument.
     * Saved in internal files directory for permanent offline accessibility.
     */
    fun generateInvoicePdf(
        context: Context,
        settings: HotelSettings,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 dpi approx)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 59, 95) // Hotel Navy #1B3B5F
            style = Paint.Style.FILL
        }
        val paintGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6) // Gold accent #D97706
            style = Paint.Style.FILL
        }
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate-200 border
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val paintTableBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate-50 table header
            style = Paint.Style.FILL
        }

        val margin = 36f
        val pageWidth = 595f
        val contentWidth = pageWidth - (margin * 2)

        // --- 1. TOP HEADER BANNER ---
        canvas.drawRect(0f, 0f, pageWidth, 90f, paintHeader)

        // Hotel logo + name + slogan
        val logo = try {
            if (settings.logoUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(settings.logoUri))?.use { BitmapFactory.decodeStream(it) }
            } else if (settings.logoUri.startsWith("android.resource://")) {
                context.contentResolver.openInputStream(Uri.parse(settings.logoUri))?.use { BitmapFactory.decodeStream(it) }
            } else if (settings.logoUri.isNotBlank()) BitmapFactory.decodeFile(settings.logoUri) else null
        } catch (_: Exception) { null }
        logo?.let {
            val scale = minOf(48f / it.width, 48f / it.height)
            canvas.drawBitmap(it, null, android.graphics.RectF(margin, 10f, margin + it.width * scale, 10f + it.height * scale), null)
        }
        val headerTextX = if (logo != null) margin + 62f else margin
        paintText.color = Color.WHITE
        paintText.textSize = 20f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(settings.hotelName.uppercase(), headerTextX, 35f, paintText)
        paintText.color = Color.rgb(251, 191, 36)
        paintText.textSize = 9f
        paintText.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(settings.slogan, headerTextX, 50f, paintText)

        // Hotel contact line
        paintText.color = Color.rgb(226, 232, 240)
        paintText.textSize = 9f
        paintText.typeface = Typeface.DEFAULT
        val contactLine = listOfNotNull(
            settings.address.takeIf { it.isNotBlank() },
            settings.phone.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
            settings.gstNumber.takeIf { it.isNotBlank() }?.let { "GST: $it" }
        ).joinToString(" | ")
        canvas.drawText(contactLine, margin, 60f, paintText)

        // "TAX INVOICE" label top right
        paintText.color = Color.rgb(251, 191, 36) // Amber/Gold
        paintText.textSize = 18f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val invoiceStyle = AppPreferences.invoiceStyle(context)
        val invoiceTitle = when (invoiceStyle) {
            "RECEIPT" -> "PAYMENT RECEIPT"
            "PROFORMA" -> "PROFORMA INVOICE"
            "ESTIMATE" -> "ESTIMATE"
            else -> if (settings.gstNumber.isNotBlank()) "TAX INVOICE" else "INVOICE"
        }
        val titleWidth = paintText.measureText(invoiceTitle)
        canvas.drawText(invoiceTitle, pageWidth - margin - titleWidth, 42f, paintText)
        paintText.color = Color.rgb(226, 232, 240)
        paintText.textSize = 7f
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Professional • Offline • Itemized", pageWidth - margin - 140f, 72f, paintText)

        paintText.color = Color.WHITE
        paintText.textSize = 10f
        paintText.typeface = Typeface.DEFAULT
        val invNoText = invoice.invoiceNumber
        val invNoWidth = paintText.measureText(invNoText)
        canvas.drawText(invNoText, pageWidth - margin - invNoWidth, 60f, paintText)

        // --- 2. INVOICE META & CUSTOMER DETAILS ---
        var currentY = 120f

        // Billed To (Left)
        paintText.color = Color.rgb(100, 116, 139) // Slate-500
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO:", margin, currentY, paintText)

        paintText.color = Color.BLACK
        paintText.textSize = 12f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.customerName, margin, currentY + 16f, paintText)

        paintText.color = Color.rgb(51, 65, 85)
        paintText.textSize = 10f
        paintText.typeface = Typeface.DEFAULT
        if (invoice.customerContact.isNotBlank()) {
            canvas.drawText("Contact: ${invoice.customerContact}", margin, currentY + 30f, paintText)
        }

        // Room details under customer
        canvas.drawText("Room: ${invoice.roomNumber} (${invoice.roomType})", margin, currentY + 44f, paintText)
        canvas.drawText("Guests: ${invoice.guests}", margin, currentY + 58f, paintText)

        // Invoice Meta (Right side)
        val rightColX = pageWidth / 2 + 30f
        paintText.color = Color.rgb(100, 116, 139)
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE DETAILS:", rightColX, currentY, paintText)

        paintText.color = Color.rgb(51, 65, 85)
        paintText.textSize = 10f
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Date: ${DateUtils.formatDisplayDate(invoice.invoiceDate)}", rightColX, currentY + 16f, paintText)
        canvas.drawText("Booking ID: ${invoice.bookingId}", rightColX, currentY + 30f, paintText)
        canvas.drawText("Check-In: ${DateUtils.formatDisplayDate(invoice.checkInDate)}", rightColX, currentY + 44f, paintText)
        canvas.drawText("Check-Out: ${DateUtils.formatDisplayDate(invoice.checkOutDate)}", rightColX, currentY + 58f, paintText)

        // Status badge
        val statusText = "STATUS: ${invoice.paymentStatus}"
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = if (invoice.paymentStatus == "PAID") Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
        canvas.drawText(statusText, rightColX, currentY + 74f, paintText)

        currentY += 95f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paintLine)

        // --- 3. ITEMIZED CHARGES TABLE ---
        currentY += 15f
        // Table Header
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 24f, paintTableBg)
        paintText.color = Color.rgb(71, 85, 105)
        paintText.textSize = 10f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ITEM / DESCRIPTION", margin + 10f, currentY + 16f, paintText)
        canvas.drawText("QTY", pageWidth - margin - 220f, currentY + 16f, paintText)
        canvas.drawText("RATE", pageWidth - margin - 140f, currentY + 16f, paintText)
        canvas.drawText("AMOUNT", pageWidth - margin - 60f, currentY + 16f, paintText)

        currentY += 24f

        val tableRows = mutableListOf<Triple<String, Int, Double>>()
        if (items.isNotEmpty()) {
            items.forEach { tableRows.add(Triple(it.description, it.quantity, it.totalPrice)) }
        } else {
            if (invoice.roomCharges > 0) tableRows.add(Triple("Room Charges (${invoice.roomNumber})", 1, invoice.roomCharges))
            if (invoice.foodCharges > 0) tableRows.add(Triple("Food & Breakfast Charges", 1, invoice.foodCharges))
            if (invoice.otherCharges > 0) tableRows.add(Triple("Other Charges", 1, invoice.otherCharges))
        }

        paintText.typeface = Typeface.DEFAULT
        paintText.textSize = 10f

        tableRows.forEachIndexed { idx, row ->
            val rowY = currentY + 18f
            paintText.color = Color.rgb(30, 41, 59)
            canvas.drawText(row.first, margin + 10f, rowY, paintText)
            canvas.drawText("${row.second}", pageWidth - margin - 220f, rowY, paintText)
            canvas.drawText(CurrencyFormatter.format(row.third / row.second.coerceAtLeast(1), settings.currencySymbol), pageWidth - margin - 140f, rowY, paintText)
            canvas.drawText(CurrencyFormatter.format(row.third, settings.currencySymbol), pageWidth - margin - 60f, rowY, paintText)

            currentY += 24f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paintLine)
        }

        // --- 4. SUMMARY & TOTALS ---
        currentY += 15f
        val summaryX = pageWidth / 2 + 40f

        fun drawTotalLine(label: String, amountStr: String, isBold: Boolean = false, isHighlight: Boolean = false) {
            paintText.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paintText.color = if (isHighlight) Color.rgb(27, 59, 95) else Color.rgb(71, 85, 105)
            paintText.textSize = if (isBold) 11f else 10f
            canvas.drawText(label, summaryX, currentY, paintText)

            val amtWidth = paintText.measureText(amountStr)
            canvas.drawText(amountStr, pageWidth - margin - amtWidth, currentY, paintText)
            currentY += 18f
        }

        if (invoice.discount > 0) {
            drawTotalLine("Discount", "- ${CurrencyFormatter.format(invoice.discount, settings.currencySymbol)}")
        }
        if (invoice.tax > 0) {
            drawTotalLine("Taxes / GST", CurrencyFormatter.format(invoice.tax, settings.currencySymbol))
        }

        drawTotalLine("Grand Total", CurrencyFormatter.format(invoice.grandTotal, settings.currencySymbol), isBold = true)
        if (invoice.advance > 0) {
            drawTotalLine("Advance Paid", CurrencyFormatter.format(invoice.advance, settings.currencySymbol))
        }
        drawTotalLine("Total Paid", CurrencyFormatter.format(invoice.paid, settings.currencySymbol))

        currentY += 4f
        canvas.drawLine(summaryX, currentY, pageWidth - margin, currentY, paintLine)
        currentY += 14f

        // Pending Balance
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 12f
        paintText.color = if (invoice.pending > 0) Color.rgb(239, 68, 68) else Color.rgb(16, 185, 129)
        canvas.drawText("BALANCE DUE:", summaryX, currentY, paintText)
        val pendStr = CurrencyFormatter.format(invoice.pending, settings.currencySymbol)
        val pendWidth = paintText.measureText(pendStr)
        canvas.drawText(pendStr, pageWidth - margin - pendWidth, currentY, paintText)

        // --- 5. UPI QR CODE ---
        var qrBottom = currentY
        val upiY = 480f
        val qrBitmap = if (settings.upiId.isNotBlank()) {
            val upiUrl = QrCodeGenerator.buildUpiUrl(
                upiId = settings.upiId,
                payeeName = settings.upiPayeeName.ifBlank { settings.hotelName },
                amount = if (invoice.pending > 0) invoice.pending else null
            )
            QrCodeGenerator.generateBitmap(upiUrl, 100)
        } else {
            try {
                if (settings.manualQrUri.startsWith("content://") || settings.manualQrUri.startsWith("android.resource://")) {
                    context.contentResolver.openInputStream(Uri.parse(settings.manualQrUri))?.use { BitmapFactory.decodeStream(it) }
                } else if (settings.manualQrUri.isNotBlank()) BitmapFactory.decodeFile(settings.manualQrUri) else null
            } catch (_: Exception) { null }
        }

        qrBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, android.graphics.RectF(margin, upiY, margin + 100f, upiY + 100f), null)
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.textSize = 9f
            paintText.color = Color.rgb(27, 59, 95)
            canvas.drawText("SCAN & PAY", margin + 110f, upiY + 25f, paintText)
            paintText.typeface = Typeface.DEFAULT
            paintText.textSize = 8f
            paintText.color = Color.rgb(100, 116, 139)
            if (settings.upiId.isNotBlank()) {
                canvas.drawText("UPI ID: ${settings.upiId}", margin + 110f, upiY + 42f, paintText)
                if (invoice.pending > 0) {
                    canvas.drawText("Amount: ${CurrencyFormatter.format(invoice.pending, settings.currencySymbol)}", margin + 110f, upiY + 58f, paintText)
                }
            } else {
                canvas.drawText("Fixed hotel QR", margin + 110f, upiY + 42f, paintText)
            }
            qrBottom = upiY + 110f
        }

        // --- 6. TERMS & FOOTER ---
        val footerY = maxOf(qrBottom + 20f, 620f)
        canvas.drawText("TERMS & CONDITIONS:", margin, footerY + 16f, paintText)

        paintText.typeface = Typeface.DEFAULT
        var termY = footerY + 28f
        settings.termsAndConditions.split("\n").take(3).forEach { line ->
            canvas.drawText(line, margin, termY, paintText)
            termY += 12f
        }

        // Authorized Signature area (Bottom Right)
        val sigX = pageWidth - margin - 150f
        canvas.drawLine(sigX, footerY + 55f, pageWidth - margin, footerY + 55f, paintLine)
        paintText.color = Color.rgb(71, 85, 105)
        paintText.textSize = 9f
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Authorized Signatory", sigX + 25f, footerY + 68f, paintText)

        // Footer Message
        paintText.color = Color.rgb(148, 163, 184)
        paintText.textSize = 8f
        val footerMsg = settings.invoiceFooter
        val msgWidth = paintText.measureText(footerMsg)
        canvas.drawText(footerMsg, (pageWidth - msgWidth) / 2f, 810f, paintText)

        pdfDocument.finishPage(page)

        // Save to internal storage
        val invoicesDir = File(context.filesDir, "invoices").apply { mkdirs() }
        val pdfFile = File(invoicesDir, "${invoice.invoiceNumber}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Shares invoice PDF using system Intent via FileProvider.
     */
    fun shareInvoicePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hotel Invoice - ${pdfFile.nameWithoutExtension}")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
    }

    /**
     * Opens PDF with installed PDF viewer.
     */
    fun openInvoicePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareInvoicePdf(context, pdfFile)
        }
    }

    /**
     * Prints PDF using Android PrintManager.
     */
    fun printInvoicePdf(context: Context, pdfFile: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = java.io.FileInputStream(pdfFile)
                    val output = FileOutputStream(destination?.fileDescriptor)
                    input.copyTo(output)
                    input.close()
                    output.close()
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print("Hotel_Invoice_${pdfFile.nameWithoutExtension}", printAdapter, null)
    }
}
