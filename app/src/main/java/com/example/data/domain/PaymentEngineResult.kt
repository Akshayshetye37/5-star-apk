package com.example.data.domain

/**
 * Result of the authoritative payment engine.
 *
 * The engine only returns SUCCESS after Payment,
 * FinanceTransaction and Folio state have committed
 * inside the same Room transaction.
 */
sealed interface PaymentEngineResult {

    data class Success(
        val paymentId: String,
        val transactionId: String,
        val bookingId: String,
        val folioId: String,
        val amount: Double,
        val balance: LedgerBalance
    ) : PaymentEngineResult

    data class AlreadyProcessed(
        val paymentId: String,
        val transactionId: String,
        val bookingId: String,
        val folioId: String,
        val amount: Double
    ) : PaymentEngineResult

    data class Rejected(
        val reason: String
    ) : PaymentEngineResult
}
