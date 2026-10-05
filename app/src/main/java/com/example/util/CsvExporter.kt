package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Booking
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.FoodItem
import com.example.data.model.Invoice
import com.example.data.model.Payment
import com.example.data.model.Reservation
import com.example.data.model.RoomEntity
import java.io.File
import java.io.FileWriter

object CsvExporter {

    private fun escape(data: Any?): String {
        val s = (data ?: "").toString()
        return if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }
    }

    fun exportBookingsToCsv(context: Context, bookings: List<Booking>): File {
        val file = File(context.cacheDir, "hotel_bookings_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Booking ID,Customer Name,Phone,Room No,Room Type,Check-In Date,Check-Out Date,Guests,Room Charges,Food & Other,Discount,Grand Total,Advance,Paid,Pending,Status")
            for (b in bookings) {
                val foodTotal = b.mineralWater + b.ghavaneChatney + b.tea + b.kandePohe + b.otherCharges
                writer.appendLine(
                    listOf(
                        escape(b.bookingId),
                        escape(b.customerName),
                        escape(b.contactNumber),
                        escape(b.roomNumber),
                        escape(b.roomType),
                        escape(b.checkInDate),
                        escape(b.checkOutDate),
                        escape(b.numberOfGuests),
                        escape(b.roomCharges),
                        escape(foodTotal),
                        escape(b.discount),
                        escape(b.grandTotal),
                        escape(b.advance),
                        escape(b.paid),
                        escape(b.pending),
                        escape(b.status)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportReservationsToCsv(context: Context, reservations: List<Reservation>): File {
        val file = File(context.cacheDir, "hotel_reservations_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Reservation ID,Customer Name,Phone,Room No,Room Type,Check-In Date,Check-Out Date,Guests,Advance,Status,Notes")
            for (r in reservations) {
                writer.appendLine(
                    listOf(
                        escape(r.reservationId),
                        escape(r.customerName),
                        escape(r.phone),
                        escape(r.roomNumber),
                        escape(r.roomType),
                        escape(r.checkInDate),
                        escape(r.checkOutDate),
                        escape(r.guests),
                        escape(r.advance),
                        escape(r.status),
                        escape(r.notes)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportCustomersToCsv(context: Context, customers: List<Customer>): File {
        val file = File(context.cacheDir, "hotel_customers_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Customer ID,Name,ID Type,ID Number,Phone,WhatsApp,Address,Total Visits,Last Visit")
            for (c in customers) {
                writer.appendLine(
                    listOf(
                        escape(c.customerId),
                        escape(c.name),
                        escape(c.idType),
                        escape(c.idNumber),
                        escape(c.phone),
                        escape(c.whatsapp),
                        escape(c.address),
                        escape(c.totalVisits),
                        escape(DateUtils.formatDisplayDate(c.lastVisit))
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportRoomsToCsv(context: Context, rooms: List<RoomEntity>): File {
        val file = File(context.cacheDir, "hotel_rooms_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Room Number,Room Type,Floor,Rate,Status,Notes")
            for (r in rooms) {
                writer.appendLine(
                    listOf(
                        escape(r.roomNumber),
                        escape(r.roomType),
                        escape(r.floor),
                        escape(r.rate),
                        escape(r.status),
                        escape(r.notes)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportInvoicesToCsv(context: Context, invoices: List<Invoice>): File {
        val file = File(context.cacheDir, "hotel_invoices_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Invoice No,Booking ID,Date,Customer,Room,Grand Total,Paid,Pending,Status,Payment Method")
            for (inv in invoices) {
                writer.appendLine(
                    listOf(
                        escape(inv.invoiceNumber),
                        escape(inv.bookingId),
                        escape(DateUtils.formatDisplayDate(inv.invoiceDate)),
                        escape(inv.customerName),
                        escape(inv.roomNumber),
                        escape(inv.grandTotal),
                        escape(inv.paid),
                        escape(inv.pending),
                        escape(inv.paymentStatus),
                        escape(inv.paymentMethod)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportPaymentsToCsv(context: Context, payments: List<Payment>): File {
        val file = File(context.cacheDir, "hotel_payments_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Payment ID,Booking ID,Customer,Amount,Date,Method,Ref Number,Notes")
            for (p in payments) {
                writer.appendLine(
                    listOf(
                        escape(p.paymentId),
                        escape(p.bookingId),
                        escape(p.customerName),
                        escape(p.amount),
                        escape(DateUtils.formatDisplayDateTime(p.date)),
                        escape(p.paymentMethod),
                        escape(p.referenceNumber),
                        escape(p.notes)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun exportExpensesToCsv(context: Context, expenses: List<Expense>): File {
        val file = File(context.cacheDir, "hotel_expenses_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("Expense ID,Date,Category,Description,Amount,Payment Method,Notes")
            for (e in expenses) {
                writer.appendLine(
                    listOf(
                        escape(e.expenseId),
                        escape(DateUtils.formatDisplayDate(e.date)),
                        escape(e.category),
                        escape(e.description),
                        escape(e.amount),
                        escape(e.paymentMethod),
                        escape(e.notes)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    fun shareCsvFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share Export CSV"))
    }
}
