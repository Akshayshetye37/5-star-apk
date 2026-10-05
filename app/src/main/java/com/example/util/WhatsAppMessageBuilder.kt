package com.example.util

import com.example.data.model.Booking

object WhatsAppMessageBuilder {
    fun checkIn(booking: Booking, hotelName: String): String = """
        Hello ${booking.customerName},

        Welcome to $hotelName.
        Your booking is confirmed:
        Room: ${booking.roomNumber} (${booking.roomType})
        Check-in: ${booking.checkInDate} ${booking.checkInTime}
        Check-out: ${booking.checkOutDate} ${booking.checkOutTime}
        Booking ID: ${booking.bookingId}

        Thank you and have a pleasant stay.
    """.trimIndent()

    fun pending(booking: Booking, currency: String): String = """
        Hello ${booking.customerName},

        Payment reminder from hotel:
        Guest: ${booking.customerName}
        Room: ${booking.roomNumber}
        Booking ID: ${booking.bookingId}

        Room charges: ${CurrencyFormatter.format(booking.roomCharges, currency)}
        Food charges: ${CurrencyFormatter.format(
            booking.mineralWater + booking.ghavaneChatney + booking.tea + booking.kandePohe, currency
        )}
        Other charges: ${CurrencyFormatter.format(booking.otherCharges, currency)}
        Discount: ${CurrencyFormatter.format(booking.discount, currency)}
        Total bill: ${CurrencyFormatter.format(booking.grandTotal, currency)}
        Paid: ${CurrencyFormatter.format(booking.paid, currency)}

        Amount requested: ${CurrencyFormatter.format(booking.pending, currency)}
        Reason: outstanding hotel bill balance for Room ${booking.roomNumber}.

        Please use the QR in the attached payment PDF or contact us if you have any questions.
    """.trimIndent()
}
