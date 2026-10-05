package com.example.domain.upi

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class UpiPaymentAnnouncementService(
    context: Context
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext

    private var textToSpeech: TextToSpeech? = null
    private var initialized = false

    init {
        textToSpeech = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            val result = tts.setLanguage(Locale("en", "IN"))
            initialized = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun announceReceivedAmount(
        amount: Double,
        currencySymbol: String = "₹"
    ) {
        if (!initialized || amount <= 0.0) return

        val formattedAmount = String.format(
            Locale.US,
            "%.2f",
            amount
        )

        val message = "Payment received. $currencySymbol $formattedAmount."

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "upi-payment-$formattedAmount"
        )
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        initialized = false
    }
}
