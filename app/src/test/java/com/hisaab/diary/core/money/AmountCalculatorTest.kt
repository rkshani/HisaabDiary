package com.hisaab.diary.core.money

import com.hisaab.diary.core.money.AmountCalculator.DIVIDE
import com.hisaab.diary.core.money.AmountCalculator.MINUS
import com.hisaab.diary.core.money.AmountCalculator.PLUS
import com.hisaab.diary.core.money.AmountCalculator.TIMES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountCalculatorTest {
    @Test
    fun `plain numbers`() {
        assertEquals(Money(125_000), AmountCalculator.evaluate("1250"))
        assertEquals(Money(125_050), AmountCalculator.evaluate("1250.5"))
        assertEquals(Money(5), AmountCalculator.evaluate("0.05"))
    }

    @Test
    fun `operators with precedence`() {
        assertEquals(Money.ofRupees(1_900), AmountCalculator.evaluate("1200${PLUS}350${TIMES}2"))
        assertEquals(Money.ofRupees(50), AmountCalculator.evaluate("100${MINUS}25${TIMES}2"))
        assertEquals(Money.ofRupees(25), AmountCalculator.evaluate("100${DIVIDE}4"))
        assertEquals(Money.ofRupees(7), AmountCalculator.evaluate("1${PLUS}2${TIMES}3"))
    }

    @Test
    fun `division rounds to paisa`() {
        assertEquals(Money(3_333), AmountCalculator.evaluate("100${DIVIDE}3"))
        assertEquals(Money(6_667), AmountCalculator.evaluate("200${DIVIDE}3"))
    }

    @Test
    fun `invalid or incomplete gives null`() {
        assertNull(AmountCalculator.evaluate(""))
        assertNull(AmountCalculator.evaluate("12$PLUS"))
        assertNull(AmountCalculator.evaluate("5${DIVIDE}0"))
        assertNull(AmountCalculator.evaluate("5${MINUS}10"))
        assertNull(AmountCalculator.evaluate("9999999999999${TIMES}9999999"))
    }

    @Test
    fun `append keeps text valid`() {
        assertEquals("", AmountCalculator.append("", PLUS))
        assertEquals("0.", AmountCalculator.append("", '.'))
        assertEquals("1.5", AmountCalculator.append("1.5", '.'))
        assertEquals("1.55", AmountCalculator.append("1.55", '7'))
        assertEquals("7", AmountCalculator.append("0", '7'))
        assertEquals("12$TIMES", AmountCalculator.append("12$PLUS", TIMES))
        assertEquals("12${PLUS}0.", AmountCalculator.append("12$PLUS", '.'))
        assertEquals("12${PLUS}3", AmountCalculator.append("12$PLUS", '3'))
        assertEquals("12", AmountCalculator.append("12", 'x'))
    }

    @Test
    fun `operator detection`() {
        assertTrue(AmountCalculator.hasOperator("1${PLUS}2"))
        assertFalse(AmountCalculator.hasOperator("12.5"))
    }
}
