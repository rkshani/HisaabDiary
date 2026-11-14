package com.hisaab.diary.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `rupees and paisa parts`() {
        val money = Money(125_050)
        assertEquals(1250L, money.rupees)
        assertEquals(50, money.paisaPart)
        assertEquals(50, Money(-125_050).paisaPart)
    }

    @Test
    fun `math is exact`() {
        assertEquals(Money(30), Money(10) + Money(20))
        assertEquals(Money(-10), Money(10) - Money(20))
        assertEquals(Money(5), -Money(-5))
        assertEquals(Money(15), Money(-15).abs())
        assertEquals(Money(60), listOf(Money(10), Money(20), Money(30)).sum())
        assertEquals(Money.ZERO, emptyList<Money>().sum())
    }

    @Test
    fun `ofRupees converts to paisa`() {
        assertEquals(Money(250_000), Money.ofRupees(2_500))
    }

    @Test
    fun `sign checks`() {
        assertTrue(Money.ZERO.isZero)
        assertTrue(Money(1).isPositive)
        assertTrue(Money(-1).isNegative)
        assertFalse(Money(-1).isPositive)
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow throws instead of wrapping`() {
        Money(Long.MAX_VALUE) + Money(1)
    }

    @Test
    fun `compare amounts`() {
        assertTrue(Money(100) > Money(99))
        assertEquals(Money(5), listOf(Money(9), Money(5), Money(7)).min())
    }
}
