package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Authoritative financial container for one live booking.
 *
 * Financial truth is derived from FolioCharge + Payment.
 * Never use Booking.paid as the authoritative ledger balance.
 */
@Entity(
    tableName = "folios",
    indices = [
        Index(value = ["bookingId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class Folio(
    @PrimaryKey
    val folioId: String,
    val bookingId: String,
    val status: String = "OPEN", // OPEN, CLOSED, VOID
    val currencyCode: String = "INR",
    val version: Int = 1,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable-ish snapshot of a financial charge.
 *
 * Historical invoices must use these values rather than current
 * menu/room/rate values.
 */
@Entity(
    tableName = "folio_charges",
    indices = [
        Index(value = ["folioId"]),
        Index(value = ["bookingId"]),
        Index(value = ["chargeType"]),
        Index(value = ["createdAt"])
    ]
)
data class FolioCharge(
    @PrimaryKey
    val chargeId: String,
    val folioId: String,
    val bookingId: String,
    val chargeType: String, // ROOM, FOOD, EXTRA, TAX, DISCOUNT, ADVANCE, ADJUSTMENT
    val sourceId: String = "",
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val grossAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = ""
)

/**
 * Accounting posting generated from a payment or other financial event.
 *
 * paymentId is unique for payment-generated transactions so one
 * payment cannot accidentally post twice.
 */
@Entity(
    tableName = "finance_transactions",
    indices = [
        Index(value = ["paymentId"], unique = true),
        Index(value = ["bookingId"]),
        Index(value = ["folioId"]),
        Index(value = ["transactionType"]),
        Index(value = ["transactionTimestamp"])
    ]
)
data class FinanceTransaction(
    @PrimaryKey
    val transactionId: String,
    val paymentId: String? = null,
    val bookingId: String = "",
    val folioId: String = "",
    val transactionType: String, // PAYMENT, REFUND, TIP, OTHER_INCOME, ADJUSTMENT, EXPENSE
    val paymentMethod: String = "",
    val amount: Double,
    val debitAccount: String,
    val creditAccount: String,
    val referenceNumber: String = "",
    val description: String = "",
    val transactionTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val lastModifiedTimestamp: Long = System.currentTimeMillis()
)
