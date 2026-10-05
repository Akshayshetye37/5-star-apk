package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.repository.FullDatabaseExport
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DataExportManager {
    data class Row(
        val type: String,
        val id: String,
        val name: String,
        val room: String,
        val amount: String,
        val date: String,
        val details: String
    )

    private fun rows(data: FullDatabaseExport, selected: Set<String>): List<Row> {
        val out = mutableListOf<Row>()
        if ("Bookings" in selected) data.bookings.forEach {
            out += Row("Booking", it.bookingId, it.customerName, it.roomNumber,
                CurrencyFormatter.format(it.grandTotal, data.settings.currencySymbol),
                "${it.checkInDate} → ${it.checkOutDate}",
                "Guests=${it.numberOfGuests}; Paid=${it.paid}; Pending=${it.pending}")
        }
        if ("Guests" in selected) data.bookingGuests.forEach {
            out += Row("Guest", it.guestId, it.name, data.bookings.find { b -> b.bookingId == it.bookingId }?.roomNumber.orEmpty(),
                "", "", "Phone=${it.phone}; WhatsApp=${it.whatsapp}; ID=${it.idType}:${it.idNumber}; Vehicles=${it.vehicleNumbers.replace("\n", ", ")}")
        }
        if ("Rooms" in selected) data.rooms.forEach {
            out += Row("Room", it.roomNumber, it.roomType, it.roomNumber, CurrencyFormatter.format(it.rate, data.settings.currencySymbol),
                "", "Floor=${it.floor}; Status=${it.status}")
        }
        if ("Payments" in selected) data.payments.forEach {
            out += Row("Payment", it.paymentId, it.customerName, data.bookings.find { b -> b.bookingId == it.bookingId }?.roomNumber.orEmpty(),
                CurrencyFormatter.format(it.amount, data.settings.currencySymbol), DateUtils.formatDisplayDateTime(it.date),
                "${it.paymentMethod}; Ref=${it.referenceNumber}; Notes=${it.notes}")
        }
        if ("Invoices" in selected) data.invoices.forEach {
            out += Row("Invoice", it.invoiceNumber, it.customerName, it.roomNumber,
                CurrencyFormatter.format(it.grandTotal, data.settings.currencySymbol), DateUtils.formatDisplayDateTime(it.invoiceDate),
                "Paid=${it.paid}; Pending=${it.pending}; Status=${it.paymentStatus}")
        }
        if ("Reservations" in selected) data.reservations.forEach {
            out += Row("Reservation", it.reservationId, it.customerName, it.roomNumber, CurrencyFormatter.format(it.advance, data.settings.currencySymbol),
                "${it.checkInDate} → ${it.checkOutDate}", "Status=${it.status}; Guests=${it.guests}")
        }
        if ("Food" in selected) data.foodOrders.forEach {
            out += Row("Food Order", "${it.bookingId}-${it.orderTimestamp}", it.foodName, it.roomNumber,
                CurrencyFormatter.format(it.total, data.settings.currencySymbol), DateUtils.formatDisplayDateTime(it.orderTimestamp),
                "Qty=${it.quantity}; Notes=${it.notes}")
        }
        if ("Expenses" in selected) data.expenses.forEach {
            out += Row("Expense", it.expenseId, it.description, "", CurrencyFormatter.format(it.amount, data.settings.currencySymbol),
                DateUtils.formatDisplayDateTime(it.date), "${it.category}; ${it.paymentMethod}; ${it.notes}")
        }
        return out
    }

    fun export(context: Context, data: FullDatabaseExport, selected: Set<String>, format: String): File {
        val rows = rows(data, selected)
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        return when (format.uppercase()) {
            "XLSX" -> exportXlsx(File(dir, "hotel_export_$stamp.xlsx"), rows)
            "DOCX" -> exportDocx(File(dir, "hotel_export_$stamp.docx"), rows)
            else -> exportPdf(File(dir, "hotel_export_$stamp.pdf"), rows, data.settings.hotelName)
        }
    }

    private fun xmlEscape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun writeZip(file: File, entries: List<Pair<String, String>>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun exportXlsx(file: File, rows: List<Row>): File {
        val all = listOf(Row("Type", "ID", "Name", "Room", "Amount", "Date", "Details")) + rows
        fun cell(value: String, ref: String) =
            """<c r="$ref" t="inlineStr"><is><t>${xmlEscape(value)}</t></is></c>"""
        val body = buildString {
            all.forEachIndexed { r, row ->
                append("<row r=\"${r + 1}\">")
                listOf(row.type, row.id, row.name, row.room, row.amount, row.date, row.details)
                    .forEachIndexed { c, value ->
                        val col = ('A'.code + c).toChar()
                        append(cell(value, "$col${r + 1}"))
                    }
                append("</row>")
            }
        }
        val sheet = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$body</sheetData></worksheet>"""
        val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Hotel Data" sheetId="1" r:id="rId1"/></sheets></workbook>"""
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>"""
        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>"""
        writeZip(file, listOf(
            "[Content_Types].xml" to content,
            "_rels/.rels" to rootRels,
            "xl/workbook.xml" to workbook,
            "xl/_rels/workbook.xml.rels" to rels,
            "xl/worksheets/sheet1.xml" to sheet
        ))
        return file
    }

    private fun exportDocx(file: File, rows: List<Row>): File {
        val all = listOf(Row("Type", "ID", "Name", "Room", "Amount", "Date", "Details")) + rows
        val table = buildString {
            append("""<w:tbl xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            all.forEach { row ->
                append("<w:tr>")
                listOf(row.type, row.id, row.name, row.room, row.amount, row.date, row.details).forEach { value ->
                    append("<w:tc><w:p><w:r><w:t>${xmlEscape(value)}</w:t></w:r></w:p></w:tc>")
                }
                append("</w:tr>")
            }
            append("</w:tbl>")
        }
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>Hotel Billing Export</w:t></w:r></w:p>$table<w:sectPr/></w:body></w:document>"""
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"></Relationships>"""
        writeZip(file, listOf("[Content_Types].xml" to content, "_rels/.rels" to rootRels, "word/document.xml" to document, "word/_rels/document.xml.rels" to docRels))
        return file
    }

    private fun exportPdf(file: File, rows: List<Row>, hotelName: String): File {
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.BLACK }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; color = Color.BLACK; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNo).create())
        var canvas: Canvas = page.canvas
        var y = 35f
        canvas.drawText(hotelName.ifBlank { "Hotel Billing" }, 30f, y, heading)
        y += 24f
        val headers = listOf("Type", "ID", "Name", "Room", "Amount", "Date", "Details")
        canvas.drawText(headers.joinToString(" | ") { it.take(12) }, 30f, y, paint)
        y += 16f
        rows.forEach { row ->
            if (y > 560f) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNo).create())
                canvas = page.canvas
                y = 30f
            }
            val line = listOf(row.type, row.id, row.name, row.room, row.amount, row.date, row.details)
                .joinToString(" | ").take(170)
            canvas.drawText(line, 30f, y, paint)
            y += 14f
        }
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }


    /**
     * Exports a workbook compatible with the hotel's existing Booking workbook.
     * One row is produced per guest; financial values are placed on the first
     * guest row for a booking, matching the supplied workbook's convention.
     */
    fun exportHotelWorkbook(
        context: Context,
        data: FullDatabaseExport,
        fromDate: String? = null,
        toDate: String? = null,
        bookingIds: Set<String> = emptySet(),
        customerNameQuery: String? = null
    ): File {
        fun inRange(date: String): Boolean {
            val fromOk = fromDate.isNullOrBlank() || date >= fromDate
            val toOk = toDate.isNullOrBlank() || date <= toDate
            return fromOk && toOk
        }

        val filteredBookings = data.bookings.filter {
            inRange(it.checkInDate) &&
                (bookingIds.isEmpty() || it.bookingId in bookingIds) &&
                (customerNameQuery.isNullOrBlank() || it.customerName.contains(customerNameQuery.trim(), ignoreCase = true))
        }
        val rows = buildString {
            append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Hari Om Retreat</t></is></c></row>")
            append("<row r=\"2\"></row><row r=\"3\"></row><row r=\"4\">")
            append("<c r=\"A4\" t=\"inlineStr\"><is><t>Hotel Billing Export</t></is></c></row>")
            val headers = listOf(
                "S. No.", "Customer Name", "ID Type", "ID No.", "Contact No", "WhatsApp",
                "Check in Date", "Check Out Date", "Stay", "Advance", "Paid", "Pending",
                "Room No.", "Suite Type", "Room Charges", "Discount", "Mineral Water",
                "Ghavane chatney", "Tea", "Kande Pohe", "Total", "Paid", "ID'S Photo"
            )
            append("<row r=\"5\">")
            headers.forEachIndexed { i, h ->
                val col = excelColumn(i + 1)
                append(inlineCell(h, "$col" + "5"))
            }
            append("</row>")

            var outRow = 6
            filteredBookings.forEach { booking ->
                val guests = data.bookingGuests.filter { it.bookingId == booking.bookingId }
                    .sortedBy { it.sequence }
                    .ifEmpty {
                        listOf(com.example.data.model.BookingGuest(
                            guestId = "LEGACY-${booking.bookingId}",
                            bookingId = booking.bookingId,
                            sequence = 1,
                            name = booking.customerName,
                            idType = booking.idType,
                            idNumber = booking.idNumber,
                            phone = booking.contactNumber,
                            whatsapp = booking.whatsappNumber,
                            idPhotoUri = booking.idPhotoUri
                        ))
                    }
                guests.forEachIndexed { index, guest ->
                    val first = index == 0
                    val values = listOf(
                        if (first) booking.bookingId else "",
                        guest.name,
                        guest.idType,
                        guest.idNumber,
                        guest.phone,
                        guest.whatsapp,
                        booking.checkInDate,
                        booking.checkOutDate,
                        if (first) booking.numberOfGuests.toString() else "",
                        if (first) booking.advance.toString() else "",
                        if (first) booking.paid.toString() else "",
                        if (first) booking.pending.toString() else "",
                        booking.roomNumber,
                        booking.roomType,
                        if (first) booking.roomCharges.toString() else "0",
                        if (first) booking.discount.toString() else "0",
                        if (first) booking.mineralWater.toString() else "0",
                        if (first) booking.ghavaneChatney.toString() else "0",
                        if (first) booking.tea.toString() else "0",
                        if (first) booking.kandePohe.toString() else "0",
                        if (first) booking.grandTotal.toString() else "0",
                        if (first) booking.paid.toString() else "0",
                        guest.idPhotoUri
                    )
                    append("<row r=\"$outRow\">")
                    values.forEachIndexed { i, value ->
                        append(inlineCell(value, "${excelColumn(i + 1)}$outRow"))
                    }
                    append("</row>")
                    outRow++
                }
            }
        }

        val paymentsRows = buildString {
            append("<row r=\"1\">")
            listOf("Payment ID","Booking ID","Guest","Room","Amount","Date","Method","Reference","Notes")
                .forEachIndexed { i, h -> append(inlineCell(h, "${excelColumn(i+1)}1")) }
            append("</row>")
            data.payments.filter {
                val booking = data.bookings.firstOrNull { b -> b.bookingId == it.bookingId }
                booking == null || inRange(booking.checkInDate)
            }.forEachIndexed { idx, p ->
                val row = idx + 2
                val booking = data.bookings.firstOrNull { b -> b.bookingId == p.bookingId }
                val values = listOf(
                    p.paymentId, p.bookingId, p.customerName, booking?.roomNumber.orEmpty(),
                    p.amount.toString(), DateUtils.formatDisplayDateTime(p.date),
                    p.paymentMethod, p.referenceNumber, p.notes
                )
                append("<row r=\"$row\">")
                values.forEachIndexed { i, v -> append(inlineCell(v, "${excelColumn(i+1)}$row")) }
                append("</row>")
            }
        }

        val reservationsRows = buildString {
            append("<row r=\"1\">")
            listOf("Reservation ID","Guest","Contact","Check-In","Check-Out","Nights","Room","Adults","Kids","Room Charges","Advance","Balance","Status","Remarks")
                .forEachIndexed { i, h -> append(inlineCell(h, "${excelColumn(i+1)}1")) }
            append("</row>")
            data.reservations.filter { inRange(it.checkInDate) }.forEachIndexed { idx, r ->
                val row = idx + 2
                val values = listOf(r.reservationId,r.customerName,r.phone,r.checkInDate,r.checkOutDate,
                    r.guests.toString(),r.roomNumber,"","","",r.advance.toString(),"",r.status,r.notes)
                append("<row r=\"$row\">")
                values.forEachIndexed { i,v -> append(inlineCell(v,"${excelColumn(i+1)}$row")) }
                append("</row>")
            }
        }

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "hotel_billing_workbook_${System.currentTimeMillis()}.xlsx")
        val content = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()
        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="Booking" sheetId="1" r:id="rId1"/>
                <sheet name="Payments" sheetId="2" r:id="rId2"/>
                <sheet name="Reservation" sheetId="3" r:id="rId3"/>
              </sheets>
            </workbook>
        """.trimIndent()
        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
            </Relationships>
        """.trimIndent()
        val rootRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
        fun sheet(body: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$body</sheetData></worksheet>"""
        writeZip(file, listOf(
            "[Content_Types].xml" to content,
            "_rels/.rels" to rootRels,
            "xl/workbook.xml" to workbook,
            "xl/_rels/workbook.xml.rels" to rels,
            "xl/worksheets/sheet1.xml" to sheet(rows),
            "xl/worksheets/sheet2.xml" to sheet(paymentsRows),
            "xl/worksheets/sheet3.xml" to sheet(reservationsRows)
        ))
        return file
    }

    private fun inlineCell(value: String, ref: String): String =
        "<c r=\"$ref\" t=\"inlineStr\"><is><t>${xmlEscape(value)}</t></is></c>"

    private fun excelColumn(number: Int): String {
        var n = number
        val out = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            out.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return out.reverse().toString()
    }

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mime
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share Export"
        ))
    }
}
