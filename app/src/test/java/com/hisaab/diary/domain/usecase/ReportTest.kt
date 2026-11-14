package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val dateUtils = DateUtils(clock = { 0 }, timeZone = utc)

    @Test
    fun `totals and daily buckets without gaps`() {
        val from = DAY * 10
        val to = DAY * 13 - 1 // 3 days
        val entries = listOf(
            entry(EntryType.GAVE, 1_000, DAY * 10 + 5),
            entry(EntryType.GOT, 300, DAY * 10 + 9),
            entry(EntryType.GOT, 700, DAY * 12),
            entry(EntryType.GAVE, 999, DAY * 20), // outside
        )
        val report = buildReport(entries, from, to, dateUtils)
        assertEquals(Money(1_000), report.totalGave)
        assertEquals(Money(1_000), report.totalGot)
        assertEquals(Money.ZERO, report.net)
        assertFalse(report.byMonth)
        assertEquals(listOf(DAY * 10, DAY * 11, DAY * 12), report.buckets.map { it.start })
        assertEquals(listOf(1_000L, 0L, 0L), report.buckets.map { it.gave.paisa })
        assertEquals(listOf(300L, 0L, 700L), report.buckets.map { it.got.paisa })
    }

    @Test
    fun `long ranges are grouped by month`() {
        val from = date(2026, Calendar.JANUARY, 1)
        val to = date(2026, Calendar.MARCH, 31) + DAY - 1
        val entries = listOf(
            entry(EntryType.GAVE, 100, date(2026, Calendar.JANUARY, 15)),
            entry(EntryType.GAVE, 50, date(2026, Calendar.MARCH, 2)),
        )
        val report = buildReport(entries, from, to, dateUtils)
        assertTrue(report.byMonth)
        assertEquals(3, report.buckets.size)
        assertEquals(listOf(100L, 0L, 50L), report.buckets.map { it.gave.paisa })
    }

    @Test
    fun `empty range has empty totals`() {
        val report = buildReport(emptyList(), DAY, DAY * 2 - 1, dateUtils)
        assertEquals(Money.ZERO, report.totalGave)
        assertEquals(1, report.buckets.size)
    }

    private fun entry(type: EntryType, paisa: Long, date: Long) =
        Entry(0, 1, type, Money(paisa), null, date, null, null, 0, 0)

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day)
    }.timeInMillis

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
