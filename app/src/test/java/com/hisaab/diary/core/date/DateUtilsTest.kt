package com.hisaab.diary.core.date

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {
    private val karachi = TimeZone.getTimeZone("Asia/Karachi")

    // Sat 14 Nov 2026, 15:30:20.500 in Karachi (UTC+5)
    private val now = millis(2026, Calendar.NOVEMBER, 14, 15, 30, 20, 500)
    private val utils = DateUtils(clock = { now }, timeZone = karachi)

    @Test
    fun `start and end of day`() {
        assertEquals(millis(2026, Calendar.NOVEMBER, 14), utils.startOfDay(now))
        assertEquals(millis(2026, Calendar.NOVEMBER, 15) - 1, utils.endOfDay(now))
        assertEquals(utils.startOfDay(now), utils.startOfToday())
    }

    @Test
    fun `week starts on Monday`() {
        assertEquals(millis(2026, Calendar.NOVEMBER, 9), utils.startOfWeek(now))
        assertEquals(millis(2026, Calendar.NOVEMBER, 16) - 1, utils.endOfWeek(now))
        val monday = millis(2026, Calendar.NOVEMBER, 9, 8)
        assertEquals(millis(2026, Calendar.NOVEMBER, 9), utils.startOfWeek(monday))
        val sunday = millis(2026, Calendar.NOVEMBER, 15, 23)
        assertEquals(millis(2026, Calendar.NOVEMBER, 9), utils.startOfWeek(sunday))
    }

    @Test
    fun `start and end of month`() {
        assertEquals(millis(2026, Calendar.NOVEMBER, 1), utils.startOfMonth(now))
        assertEquals(millis(2026, Calendar.DECEMBER, 1) - 1, utils.endOfMonth(now))
        val leapFeb = millis(2028, Calendar.FEBRUARY, 10)
        assertEquals(millis(2028, Calendar.MARCH, 1) - 1, utils.endOfMonth(leapFeb))
    }

    @Test
    fun `same day and today`() {
        assertTrue(utils.isToday(millis(2026, Calendar.NOVEMBER, 14, 0, 0, 1)))
        assertFalse(utils.isToday(millis(2026, Calendar.NOVEMBER, 13, 23, 59, 59)))
    }

    @Test
    fun `days from today`() {
        assertEquals(0, utils.daysFromToday(now))
        assertEquals(1, utils.daysFromToday(millis(2026, Calendar.NOVEMBER, 15, 0, 1)))
        assertEquals(-14, utils.daysFromToday(millis(2026, Calendar.OCTOBER, 31, 23)))
        assertEquals(365, utils.daysFromToday(millis(2027, Calendar.NOVEMBER, 14)))
    }

    @Test
    fun `days from today and plusDays across daylight saving`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val base = millis(2026, Calendar.MARCH, 28, 12, zone = london)
        val dst = DateUtils(clock = { base }, timeZone = london)
        assertEquals(2, dst.daysFromToday(millis(2026, Calendar.MARCH, 30, 0, 30, zone = london)))
        assertEquals(millis(2026, Calendar.MARCH, 30, 12, zone = london), dst.plusDays(base, 2))
    }

    @Suppress("LongParameterList")
    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        ms: Int = 0,
        zone: TimeZone = karachi,
    ): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, minute, second)
        set(Calendar.MILLISECOND, ms)
    }.timeInMillis
}
