package com.example.data.usecase

import com.example.data.dao.FolioDao
import com.example.data.dao.FolioChargeDao
import com.example.data.dao.PaymentDao
import com.example.data.domain.LedgerBalance
import com.example.data.domain.PaymentStatus
import kotlin.math.abs

/**
 * Authoritative folio balance:
 *
 * Folio -> Booking -> Payments
 * Folio -> FolioCharges
 *
 * Balance = charges - payments.
 */
class GetLedgerBalanceUseCase(
    private val folioDao: FolioDao,
    private val folioChargeDao: FolioChargeDao,
    private val paymentDao: PaymentDao
) {
    suspend operator fun invoke(folioId: String): LedgerBalance {
        val id = folioId.trim()
        require(id.isNotBlank()) { "Folio ID is required." }

        val folio = folioDao.getById(id)
            ?: error("Folio not found: $id")

        val charges = folioChargeDao.getForFolioDirect(id)
        val payments = paymentDao.getPaymentsForBookingDirect(folio.bookingId)

        val totalCharges = charges.sumOf { it.netAmount }
        val totalPayments = payments
            .filter { it.amount > 0.0 }
            .sumOf { it.amount }

        val balance = totalCharges - totalPayments

        val status = when {
            totalCharges <= 0.0 && totalPayments <= 0.0 ->
                PaymentStatus.FULL
            totalPayments <= 0.0 ->
                PaymentStatus.PENDING
            totalPayments < totalCharges ->
                PaymentStatus.PARTIAL
            abs(totalPayments - totalCharges) < 0.005 ->
                PaymentStatus.FULL
            else ->
                PaymentStatus.OVERPAYMENT
        }

        return LedgerBalance(
            totalCharges = totalCharges,
            totalPayments = totalPayments,
            balance = balance,
            status = status
        )
    }
}
