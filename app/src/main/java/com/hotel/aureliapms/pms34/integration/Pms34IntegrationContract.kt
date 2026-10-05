package com.hotel.aureliapms.pms34.integration

import com.hotel.aureliapms.pms34.domain.*

/**
 * Adapter contract. Implementations must delegate to the existing application's
 * HotelDatabase/Repository instead of keeping a second in-memory source of truth.
 */
interface Pms34IntegrationContract {
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
