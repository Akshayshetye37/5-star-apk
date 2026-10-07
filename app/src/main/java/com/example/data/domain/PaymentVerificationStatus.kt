package com.example.data.domain

/**
 * Authorization/verification lifecycle of an individual payment.
 *
 * This is separate from ledger balance status and invoice settlement status.
 */
enum class PaymentVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    FAILED,
    CANCELLED,
    REVERSED
}
