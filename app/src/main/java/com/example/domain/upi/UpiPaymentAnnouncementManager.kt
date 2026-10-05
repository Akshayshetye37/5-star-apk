package com.example.domain.upi

import android.content.Context

object UpiPaymentAnnouncementManager {

    @Volatile
    private var service: UpiPaymentAnnouncementService? = null

    fun initialize(context: Context) {
        if (service == null) {
            synchronized(this) {
                if (service == null) {
                    service = UpiPaymentAnnouncementService(
                        context.applicationContext
                    )
                }
            }
        }
    }

    fun announceReceivedAmount(
        amount: Double,
        currencySymbol: String
    ) {
        service?.announceReceivedAmount(
            amount = amount,
            currencySymbol = currencySymbol
        )
    }

    fun shutdown() {
        synchronized(this) {
            service?.shutdown()
            service = null
        }
    }
}
