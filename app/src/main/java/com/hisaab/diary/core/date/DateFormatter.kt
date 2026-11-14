package com.hisaab.diary.core.date

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Turns epoch millis into text for the UI. English month names, e.g. `14 Nov 2026`. */
class DateFormatter(
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val locale: Locale = Locale.ENGLISH,
) {
    /** `14 Nov 2026` */
    fun date(millis: Long): String = pattern("d MMM yyyy").format(Date(millis))

    /** `14 Nov` (for lists in the current year) */
    fun shortDate(millis: Long): String = pattern("d MMM").format(Date(millis))

    /** `14 Nov 2026, 3:05 PM` */
    fun dateTime(millis: Long): String = pattern("d MMM yyyy, h:mm a").format(Date(millis))

    /** `Nov 2026` */
    fun monthYear(millis: Long): String = pattern("MMM yyyy").format(Date(millis))

    /** `2026-11-14` for file names (CSV/PDF/backup). */
    fun fileDate(millis: Long): String = pattern("yyyy-MM-dd").format(Date(millis))

    // SimpleDateFormat is not thread-safe, so make a new one per call.
    private fun pattern(value: String) =
        SimpleDateFormat(value, locale).apply { timeZone = this@DateFormatter.timeZone }
}
