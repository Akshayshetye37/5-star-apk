package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object ReminderScheduler {
    private const val CHANNEL = "hotel_reminders"

    fun scheduleBookingReminders(context: Context, bookingId: String, guest: String, room: String, checkoutAt: Long, pending: Double, currency: String) {
        createChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        schedule(context, alarm, bookingId.hashCode().toLong(), checkoutAt - 60 * 60 * 1000L,
            "Checkout reminder", "$guest • Room $room • checkout in about 1 hour")
        if (pending > 0) {
            schedule(context, alarm, bookingId.hashCode().toLong() + 1, checkoutAt,
                "Payment pending", "$guest • Room $room • ${CurrencyFormatter.format(pending, currency)} still pending")
        }
    }

    private fun schedule(context: Context, alarm: AlarmManager, id: Long, at: Long, title: String, text: String) {
        if (at <= System.currentTimeMillis()) return
        val intent = Intent(context, HotelReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
        }
        val pi = PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Hotel reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun notifyNow(context: Context, title: String, text: String) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

class HotelReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.notifyNow(
            context,
            intent.getStringExtra("title") ?: "Hotel reminder",
            intent.getStringExtra("text") ?: ""
        )
    }
}
