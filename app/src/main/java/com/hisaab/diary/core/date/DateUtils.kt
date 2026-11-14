package com.hisaab.diary.core.date

import java.util.Calendar
import java.util.TimeZone

/** Gives the current time. Injected so tests can use a fixed time. */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

/**
 * Day/week/month math on epoch millis in a time zone.
 * Uses [Calendar] because java.time needs API 26 (minSdk is 24).
 */
class DateUtils(
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.getDefault(),
    /** First day of the week, e.g. [Calendar.MONDAY]. */
    private val firstDayOfWeek: Int = Calendar.MONDAY,
) {
    fun now(): Long = clock.nowMillis()

    fun startOfToday(): Long = startOfDay(now())

    fun startOfDay(millis: Long): Long = calendar(millis).apply { clearTime() }.timeInMillis

    /** Last millisecond of the day. */
    fun endOfDay(millis: Long): Long = calendar(millis).apply {
        clearTime()
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis - 1

    fun startOfWeek(millis: Long): Long = calendar(millis).apply {
        clearTime()
        val diff = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
        add(Calendar.DAY_OF_MONTH, -diff)
    }.timeInMillis

    fun endOfWeek(millis: Long): Long = calendar(startOfWeek(millis)).apply {
        add(Calendar.DAY_OF_MONTH, DAYS_IN_WEEK)
    }.timeInMillis - 1

    fun startOfMonth(millis: Long): Long = calendar(millis).apply {
        clearTime()
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun endOfMonth(millis: Long): Long = calendar(startOfMonth(millis)).apply {
        add(Calendar.MONTH, 1)
    }.timeInMillis - 1

    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    fun isToday(millis: Long): Boolean = isSameDay(millis, now())

    /** Whole days from today to [millis]; negative when it is in the past. */
    fun daysFromToday(millis: Long): Int {
        val from = localDayNumber(startOfToday())
        val to = localDayNumber(startOfDay(millis))
        return (to - from).toInt()
    }

    /** Whole calendar days from [from] to [to] (0 when on the same day). */
    fun daysBetween(from: Long, to: Long): Int =
        (localDayNumber(startOfDay(to)) - localDayNumber(startOfDay(from))).toInt()

    /** Same time of day, [days] later (or earlier when negative). DST-safe. */
    fun plusDays(millis: Long, days: Int): Long = calendar(millis).apply {
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

    /**
     * The Material date picker works with UTC midnight of the chosen date.
     * Returns the local start of that same calendar date.
     */
    fun fromPickerMillis(utcMillis: Long): Long {
        val utc = Calendar.getInstance(UTC).apply { timeInMillis = utcMillis }
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Opposite of [fromPickerMillis]: UTC midnight of the local date of [millis]. */
    fun toPickerMillis(millis: Long): Long {
        val local = calendar(millis)
        return Calendar.getInstance(UTC).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** [dayStart] with the time of day of now, so new entries on a past day keep a natural order. */
    fun atCurrentTime(dayStart: Long): Long = startOfDay(dayStart) + (now() - startOfToday())

    private fun calendar(millis: Long): Calendar = Calendar.getInstance(timeZone).apply {
        this.firstDayOfWeek = this@DateUtils.firstDayOfWeek
        timeInMillis = millis
    }

    /** Days since 1970-01-01 in local time (adds the zone offset, so DST does not matter). */
    private fun localDayNumber(startOfDayMillis: Long): Long =
        Math.floorDiv(startOfDayMillis + timeZone.getOffset(startOfDayMillis), MILLIS_PER_DAY)

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }
}
