package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.repository.EntryRepository
import javax.inject.Inject

/** One bar group in the chart: a day or a month. */
data class ReportBucket(val start: Long, val gave: Money, val got: Money)

/** Totals and chart data for a date range. */
data class Report(
    val totalGave: Money,
    val totalGot: Money,
    val buckets: List<ReportBucket>,
    /** True when each bucket is a month (long ranges), false when it is a day. */
    val byMonth: Boolean,
) {
    /** Positive → in this range you got more than you gave. */
    val net: Money get() = totalGot - totalGave
}

/**
 * Builds a report for entries between [from] and [to] (inclusive).
 * Ranges up to [MAX_DAY_BUCKETS] days get one bucket per day, longer ranges one per month.
 * Empty days/months are included so the chart has no gaps.
 */
fun buildReport(entries: List<Entry>, from: Long, to: Long, dateUtils: DateUtils): Report {
    val inRange = entries.filter { it.date in from..to }
    val byMonth = dateUtils.daysBetween(from, to) >= MAX_DAY_BUCKETS
    val bucketStart: (Long) -> Long = if (byMonth) dateUtils::startOfMonth else dateUtils::startOfDay
    val grouped = inRange.groupBy { bucketStart(it.date) }

    val buckets = mutableListOf<ReportBucket>()
    var cursor = bucketStart(from)
    while (cursor <= to) {
        val items = grouped[cursor].orEmpty()
        buckets += ReportBucket(cursor, items.sumOf(EntryType.GAVE), items.sumOf(EntryType.GOT))
        cursor =
            if (byMonth) dateUtils.startOfMonth(dateUtils.endOfMonth(cursor) + 1) else dateUtils.plusDays(cursor, 1)
    }
    return Report(
        totalGave = inRange.sumOf(EntryType.GAVE),
        totalGot = inRange.sumOf(EntryType.GOT),
        buckets = buckets,
        byMonth = byMonth,
    )
}

private fun List<Entry>.sumOf(type: EntryType): Money = Money(filter { it.type == type }.sumOf { it.amount.paisa })

const val MAX_DAY_BUCKETS = 32

/** Entries of a book between two dates, newest first. */
class ObserveEntriesBetweenUseCase @Inject constructor(private val entries: EntryRepository) {
    operator fun invoke(bookId: Long, from: Long, to: Long) = entries.observeEntriesBetween(bookId, from, to)
}
