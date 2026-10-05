package com.example.data.usecase

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.domain.PaymentEngineResult
import com.example.data.domain.PaymentStatus
import com.example.data.model.FinanceTransaction
import com.example.data.model.Folio
import com.example.data.model.FolioCharge
import com.example.data.model.Payment
import kotlin.math.abs
import kotlin.math.max
import java.security.MessageDigest

class RecordVerifiedPaymentUseCase(
    private val database: AppDatabase,
    private val ledgerBalanceUseCase: GetLedgerBalanceUseCase
) {
    private val bookingDao = database.bookingDao()
    private val paymentDao = database.paymentDao()
    private val invoiceDao = database.invoiceDao()
    private val folioDao = database.folioDao()
    private val folioChargeDao = database.folioChargeDao()
    private val financeTransactionDao = database.financeTransactionDao()

    suspend operator fun invoke(
        bookingId: String,
        folioId: String,
        amount: Double,
        paymentMethod: String,
        referenceNumber: String = "",
        notes: String = "",
        invoiceId: String = "",
        customerId: String = "",
        customerName: String = "",
        timestamp: Long = System.currentTimeMillis(),
        requestUuid: String,
        verified: Boolean
    ): PaymentEngineResult {

        val normalizedBookingId = bookingId.trim()
        val normalizedFolioId = folioId.trim()
        val normalizedMethod = paymentMethod.trim()
        val normalizedReference = referenceNumber.trim()
        val normalizedNotes = notes.trim()
        val normalizedInvoiceId = invoiceId.trim()
        val normalizedCustomerId = customerId.trim()
        val normalizedCustomerName = customerName.trim()

        if (!verified) {
            return PaymentEngineResult.Rejected("Payment is not verified.")
        }

        if (normalizedBookingId.isBlank()) {
            return PaymentEngineResult.Rejected("Booking ID is required.")
        }

        if (normalizedFolioId.isBlank()) {
            return PaymentEngineResult.Rejected("Folio ID is required.")
        }

        if (!amount.isFinite() || amount <= 0.0) {
            return PaymentEngineResult.Rejected(
                "Payment amount must be greater than zero."
            )
        }

        if (normalizedMethod.isBlank()) {
            return PaymentEngineResult.Rejected("Payment method is required.")
        }

        if (requestUuid.trim().isBlank()) {
            return PaymentEngineResult.Rejected(
                "Payment request ID is required."
            )
        }

        val normalizedAmount =
            kotlin.math.round(amount * 100.0) / 100.0

        if (normalizedAmount <= 0.0) {
            return PaymentEngineResult.Rejected(
                "Payment amount must be greater than zero."
            )
        }

        val paymentId = "PAY-" + stableId(
            listOf(
                normalizedBookingId,
                normalizedFolioId,
                "%.2f".format(
                    java.util.Locale.US,
                    normalizedAmount
                ),
                timestamp.toString(),
                normalizedMethod.uppercase(java.util.Locale.US),
                requestUuid.trim()
            ).joinToString("|")
        )

        val transactionId = "FT-$paymentId"

        return database.withTransaction {

            val booking =
                bookingDao.getBookingById(normalizedBookingId)
                    ?: return@withTransaction PaymentEngineResult.Rejected(
                        "Booking $normalizedBookingId was not found."
                    )

            /*
             * Idempotency.
             *
             * Replaying the same deterministic request must not
             * create another financial posting.
             */
            val existingPayment =
                paymentDao.getPaymentById(paymentId)

            val existingTransaction =
                financeTransactionDao.getById(transactionId)

            if (existingPayment != null) {
                return@withTransaction PaymentEngineResult.AlreadyProcessed(
                    paymentId = existingPayment.paymentId,
                    transactionId =
                        existingTransaction?.transactionId
                            ?: transactionId,
                    bookingId = existingPayment.bookingId,
                    folioId = normalizedFolioId,
                    amount = existingPayment.amount
                )
            }

            if (existingTransaction != null) {
                return@withTransaction PaymentEngineResult.AlreadyProcessed(
                    paymentId = paymentId,
                    transactionId = existingTransaction.transactionId,
                    bookingId = normalizedBookingId,
                    folioId = normalizedFolioId,
                    amount = normalizedAmount
                )
            }

            /*
             * Resolve or create the folio.
             */
            val folio =
                folioDao.getById(normalizedFolioId)
                    ?: folioDao.getByBookingId(normalizedBookingId)
                    ?: run {
                        val newFolio = Folio(
                            folioId = normalizedFolioId,
                            bookingId = normalizedBookingId,
                            status = "OPEN",
                            currencyCode = "INR"
                        )

                        folioDao.insert(newFolio)
                        newFolio
                    }

            if (folio.bookingId != normalizedBookingId) {
                return@withTransaction PaymentEngineResult.Rejected(
                    "Folio does not belong to this booking."
                )
            }

            if (folio.status != "OPEN") {
                return@withTransaction PaymentEngineResult.Rejected(
                    "Folio is ${folio.status} and cannot accept a payment."
                )
            }

            /*
             * Initialize the authoritative folio charge only once.
             *
             * Existing folio charges remain authoritative.
             */
            var charges =
                folioChargeDao.getForFolioDirect(folio.folioId)

            if (charges.isEmpty()) {

                val invoice =
                    invoiceDao.getInvoiceByBookingId(
                        normalizedBookingId
                    )

                val authoritativeTotal =
                    when {
                        invoice != null &&
                            invoice.grandTotal.isFinite() ->
                            invoice.grandTotal

                        booking.grandTotal.isFinite() ->
                            booking.grandTotal

                        else ->
                            0.0
                    }.coerceAtLeast(0.0)

                if (authoritativeTotal <= 0.0) {
                    return@withTransaction PaymentEngineResult.Rejected(
                        "Booking has no positive billable balance."
                    )
                }

                val initialCharge = FolioCharge(
                    chargeId = "CHG-" + stableId(
                        "${folio.folioId}|BOOKING|$normalizedBookingId"
                    ),
                    folioId = folio.folioId,
                    bookingId = normalizedBookingId,
                    chargeType = "ROOM",
                    sourceId =
                        invoice?.invoiceNumber
                            ?: normalizedBookingId,
                    description =
                        if (invoice != null) {
                            "Invoice ${invoice.invoiceNumber}"
                        } else {
                            "Booking $normalizedBookingId"
                        },
                    quantity = 1.0,
                    unitPrice = authoritativeTotal,
                    grossAmount = authoritativeTotal,
                    discountAmount = 0.0,
                    taxAmount = 0.0,
                    netAmount = authoritativeTotal,
                    occurredAt =
                        invoice?.invoiceDate
                            ?: booking.createdAt,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    lastModifiedTimestamp = timestamp,
                    deviceId = ""
                )

                folioChargeDao.insert(initialCharge)

                charges =
                    folioChargeDao.getForFolioDirect(
                        folio.folioId
                    )
            }

            /*
             * Authoritative balance before payment.
             */
            val before =
                ledgerBalanceUseCase(folio.folioId)

            val epsilon = 0.005

            if (before.balance <= epsilon) {
                return@withTransaction PaymentEngineResult.Rejected(
                    "There is no outstanding balance on this folio."
                )
            }

            if (normalizedAmount - before.balance > epsilon) {
                return@withTransaction PaymentEngineResult.Rejected(
                    "Payment exceeds the current balance. " +
                        "Explicit overpayment treatment is required."
                )
            }

            /*
             * Resolve invoice snapshot for the payment reference.
             */
            val resolvedInvoice =
                if (normalizedInvoiceId.isNotBlank()) {
                    invoiceDao.getInvoiceByNumber(
                        normalizedInvoiceId
                    )
                } else {
                    invoiceDao.getInvoiceByBookingId(
                        normalizedBookingId
                    )
                }

            val resolvedInvoiceId =
                normalizedInvoiceId.ifBlank {
                    resolvedInvoice?.invoiceNumber ?: ""
                }

            /*
             * Authoritative payment row.
             */
            val payment = Payment(
                paymentId = paymentId,
                bookingId = normalizedBookingId,
                invoiceId = resolvedInvoiceId,
                customerId =
                    normalizedCustomerId.ifBlank {
                        booking.customerId
                    },
                customerName =
                    normalizedCustomerName.ifBlank {
                        booking.customerName
                    },
                amount = normalizedAmount,
                date = timestamp,
                paymentMethod = normalizedMethod,
                referenceNumber = normalizedReference,
                notes = normalizedNotes,
                createdAt = timestamp,
                version = 1,
                lastModifiedTimestamp = timestamp,
                deviceId = ""
            )

            paymentDao.insertPayment(payment)

            /*
             * Accounting posting.
             */
            val transaction = FinanceTransaction(
                transactionId = transactionId,
                paymentId = paymentId,
                bookingId = normalizedBookingId,
                folioId = folio.folioId,
                transactionType = "PAYMENT",
                paymentMethod = normalizedMethod,
                amount = normalizedAmount,
                debitAccount =
                    paymentMethodAccount(normalizedMethod),
                creditAccount = "Pending Receivable",
                referenceNumber = normalizedReference,
                description =
                    "Verified payment for booking $normalizedBookingId",
                transactionTimestamp = timestamp,
                createdAt = timestamp,
                deviceId = "",
                lastModifiedTimestamp = timestamp
            )

            financeTransactionDao.insert(transaction)

            /*
             * Optimistic folio update.
             */
            val updatedRows =
                folioDao.updateStatusOptimistic(
                    folioId = folio.folioId,
                    expectedVersion = folio.version,
                    status = "OPEN",
                    lastModifiedTimestamp = timestamp,
                    updatedAt = timestamp
                )

            if (updatedRows != 1) {
                throw IllegalStateException(
                    "Folio changed during payment. " +
                        "Transaction rolled back."
                )
            }

            /*
             * Authoritative balance after payment.
             */
            val after =
                ledgerBalanceUseCase(folio.folioId)

            if (after.status == PaymentStatus.OVERPAYMENT) {
                throw IllegalStateException(
                    "Payment would create an overpayment. " +
                        "Transaction rolled back."
                )
            }

            if (after.balance < -epsilon) {
                throw IllegalStateException(
                    "Payment produced an invalid negative balance. " +
                        "Transaction rolled back."
                )
            }

            /*
             * Booking.paid is a compatibility projection.
             *
             * It is derived from Payment rows, never used as the
             * authoritative ledger balance.
             */
            val totalPaymentsForBooking =
                paymentDao
                    .getPaymentsForBookingDirect(
                        normalizedBookingId
                    )
                    .sumOf { it.amount }
                    .coerceAtLeast(0.0)

            bookingDao.updateBooking(
                booking.copy(
                    paid = totalPaymentsForBooking,
                    updatedAt = timestamp
                )
            )

            /*
             * Invoice payment fields are also projections.
             *
             * The invoice's historical financial snapshot
             * (grandTotal, roomCharges, tax, discount, etc.)
             * remains untouched.
             */
            if (resolvedInvoice != null) {

                val invoicePaid =
                    paymentDao
                        .getPaymentsForBookingDirect(
                            normalizedBookingId
                        )
                        .sumOf { it.amount }
                        .coerceAtLeast(0.0)

                val invoicePending =
                    max(
                        resolvedInvoice.grandTotal -
                            invoicePaid,
                        0.0
                    )

                val invoiceStatus =
                    when {
                        abs(
                            resolvedInvoice.grandTotal -
                                invoicePaid
                        ) <= epsilon ->
                            "PAID"

                        invoicePaid > epsilon ->
                            "PARTIALLY_PAID"

                        else ->
                            "PENDING"
                    }

                invoiceDao.updateInvoice(
                    resolvedInvoice.copy(
                        paid = invoicePaid,
                        pending = invoicePending,
                        paymentStatus = invoiceStatus,
                        updatedAt = timestamp
                    )
                )
            }

            PaymentEngineResult.Success(
                paymentId = paymentId,
                transactionId = transactionId,
                bookingId = normalizedBookingId,
                folioId = folio.folioId,
                amount = normalizedAmount,
                balance = after
            )
        }
    }

    private fun paymentMethodAccount(
        method: String
    ): String {
        return when {
            method.equals("Cash", ignoreCase = true) ->
                "Cash"

            method.equals("Card", ignoreCase = true) ->
                "Card Receivable"

            method.contains("UPI", ignoreCase = true) ->
                "UPI Receivable"

            method.contains("Google Pay", ignoreCase = true) ->
                "UPI Receivable"

            else ->
                "Other Payment Receivable"
        }
    }

    private fun stableId(value: String): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray(Charsets.UTF_8)
                )

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}
