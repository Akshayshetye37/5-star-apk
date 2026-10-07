package com.example.data.domain

enum class PaymentStatus {
    PENDING,
    PARTIAL,
    FULL,
    OVERPAYMENT
}

data class LedgerBalance(
    val totalCharges: Double,
    val totalPayments: Double,
    val balance: Double,
    val status: PaymentStatus
) {
    val overpayment: Double
        get() = if (balance < 0.0) -balance else 0.0

    val pendingAmount: Double
        get() = if (balance > 0.0) balance else 0.0
}
