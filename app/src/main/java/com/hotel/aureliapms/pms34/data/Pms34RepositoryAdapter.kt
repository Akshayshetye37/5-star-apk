package com.hotel.aureliapms.pms34.data

import com.hotel.aureliapms.pms34.domain.*

/**
 * Adapter boundary for wiring the 34-point domain into the existing HotelDatabase.
 *
 * Keep the current production repository/announcement wiring intact. This class
 * deliberately exposes the domain contracts without replacing the existing PMS.
 */
interface Pms34ProductionRepository {
    suspend fun dashboard(): DashboardSnapshot
    suspend fun ledgerBalance(folioId: String): Long
    suspend fun recordPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String,
        verified: Boolean
    ): Result<Payment>
    suspend fun checkout(bookingId: String): Result<Unit>
    suspend fun createInvoice(bookingId: String): Result<Invoice>
}
