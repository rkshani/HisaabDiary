package com.hisaab.diary.core.money

import kotlin.math.absoluteValue

/**
 * An amount of money in paisa (1 Rs = 100 paisa).
 * Always use this instead of Float/Double, so sums are exact.
 */
@JvmInline
value class Money(val paisa: Long) : Comparable<Money> {
    val isZero: Boolean get() = paisa == 0L
    val isPositive: Boolean get() = paisa > 0L
    val isNegative: Boolean get() = paisa < 0L

    /** Whole rupees, without the paisa part. */
    val rupees: Long get() = paisa / PAISA_PER_RUPEE

    /** Paisa part only (0..99), always positive. */
    val paisaPart: Int get() = (paisa % PAISA_PER_RUPEE).absoluteValue.toInt()

    fun abs(): Money = Money(paisa.absoluteValue)

    operator fun plus(other: Money): Money = Money(Math.addExact(paisa, other.paisa))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(paisa, other.paisa))

    operator fun unaryMinus(): Money = Money(Math.negateExact(paisa))

    override fun compareTo(other: Money): Int = paisa.compareTo(other.paisa)

    companion object {
        const val PAISA_PER_RUPEE = 100L

        val ZERO = Money(0L)

        fun ofRupees(rupees: Long): Money = Money(Math.multiplyExact(rupees, PAISA_PER_RUPEE))
    }
}

/** Sum of a list of amounts. */
fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { total, item -> total + item }
