package com.hotel.aureliapms.pms34.integration

import com.hotel.aureliapms.pms34.domain.*

/**
 * Production adapter boundary.
 *
 * This class intentionally does not fabricate a second database. Wire its
 * constructor to the existing HotelRepository/use-cases in the host project.
 */
class Pms34ProductionAdapter(
    private val delegate: ExistingPms34Delegate
) : Pms34IntegrationContract {

    override suspend fun dashboard() = delegate.dashboard()

    override suspend fun ledgerBalance(folioId: String) =
        delegate.ledgerBalance(folioId)

    override suspend fun recordVerifiedPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String
    ) = delegate.recordVerifiedPayment(
        bookingId, folioId, amountPaise, method, reference, requestUuid
    )

    override suspend fun checkout(bookingId: String) =
        delegate.checkout(bookingId)

    override suspend fun createInvoice(bookingId: String) =
        delegate.createInvoice(bookingId)

    override suspend fun transitionHousekeeping(
        taskId: String,
        next: HousekeepingStatus
    ) = delegate.transitionHousekeeping(taskId, next)
}

interface ExistingPms34Delegate {
    suspend fun dashboard(): DashboardSnapshot
    suspend fun ledgerBalance(folioId: String): Long
    suspend fun recordVerifiedPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String
    ): Result<Payment>
    suspend fun checkout(bookingId: String): Result<Unit>
    suspend fun createInvoice(bookingId: String): Result<Invoice>
    suspend fun transitionHousekeeping(
        taskId: String,
        next: HousekeepingStatus
    ): Result<Unit>
}
