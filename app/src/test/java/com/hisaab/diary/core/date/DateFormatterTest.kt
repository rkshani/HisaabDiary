package com.hisaab.diary.core.date

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {
    private val zone = TimeZone.getTimeZone("Asia/Karachi")
    private val formatter = DateFormatter(timeZone = zone)
    private val time = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 4, 15, 5)
    }.timeInMillis

    @Test
    fun `formats dates in English`() {
        assertEquals("4 Nov 2026", formatter.date(time))
        assertEquals("4 Nov", formatter.shortDate(time))
        assertEquals("4 Nov 2026, 3:05 PM", formatter.dateTime(time))
        assertEquals("Nov 2026", formatter.monthYear(time))
        assertEquals("2026-11-04", formatter.fileDate(time))
    }
}
