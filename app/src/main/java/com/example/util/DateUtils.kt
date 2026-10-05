package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DISPLAY_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val TIME_FORMAT = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val FULL_DATETIME_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val ISO_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun todayDateString(): String = DATE_FORMAT.format(Date())

    fun tomorrowDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return DATE_FORMAT.format(cal.time)
    }

    fun currentTimeString(): String = TIME_FORMAT.format(Date())

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val d = DATE_FORMAT.parse(dateStr)
            if (d != null) DISPLAY_DATE_FORMAT.format(d) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDisplayDateTime(timestamp: Long): String {
        return FULL_DATETIME_FORMAT.format(Date(timestamp))
    }

    fun formatDisplayDate(timestamp: Long): String {
        return DISPLAY_DATE_FORMAT.format(Date(timestamp))
    }

    fun parseDateTimeToTimestamp(dateStr: String, timeStr: String): Long {
        return try {
            // standard e.g. "2026-10-03" + "12:00 PM"
            val combined = "$dateStr $timeStr"
            val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            format.parse(combined)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                DATE_FORMAT.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameDay(dateStr: String, timestamp: Long): Boolean {
        return try {
            val d1 = DATE_FORMAT.parse(dateStr)
            val d2 = Date(timestamp)
            DATE_FORMAT.format(d1) == DATE_FORMAT.format(d2)
        } catch (e: Exception) {
            false
        }
    }

    fun getStartOfDay(cal: Calendar = Calendar.getInstance()): Long {
        return cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(cal: Calendar = Calendar.getInstance()): Long {
        return cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return getStartOfDay(cal)
    }

    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(cal)
    }

    fun getDaysBetween(checkInTimestamp: Long, checkOutTimestamp: Long): Int {
        val diff = checkOutTimestamp - checkInTimestamp
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        return if (days <= 0) 1 else days
    }
}
