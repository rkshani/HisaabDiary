package com.hisaab.diary.core.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    private val rs = MoneyFormatter(CurrencySymbol.RS)

    @Test
    fun `whole rupees have no decimals`() {
        assertEquals("Rs 1,250", rs.format(Money.ofRupees(1_250)))
        assertEquals("Rs 0", rs.format(Money.ZERO))
        assertEquals("Rs 999", rs.format(Money.ofRupees(999)))
    }

    @Test
    fun `paisa are shown with two digits`() {
        assertEquals("Rs 1,250.50", rs.format(Money(125_050)))
        assertEquals("Rs 0.05", rs.format(Money(5)))
    }

    @Test
    fun `groups thousands`() {
        assertEquals("Rs 1,000", rs.format(Money.ofRupees(1_000)))
        assertEquals("Rs 100,000", rs.format(Money.ofRupees(100_000)))
        assertEquals("Rs 12,345,678", rs.format(Money.ofRupees(12_345_678)))
    }

    @Test
    fun `negative shows sign only when asked`() {
        assertEquals("Rs 500", rs.format(Money.ofRupees(-500)))
        assertEquals("Rs -500", rs.format(Money.ofRupees(-500), showSign = true))
    }

    @Test
    fun `always show paisa`() {
        assertEquals("Rs 10.00", rs.format(Money.ofRupees(10), alwaysShowPaisa = true))
    }

    @Test
    fun `other symbols`() {
        assertEquals("₨ 1,250.50", MoneyFormatter(CurrencySymbol.RUPEE_SIGN).format(Money(125_050)))
        assertEquals("1,250", MoneyFormatter(CurrencySymbol.NONE).format(Money.ofRupees(1_250)))
    }

    @Test
    fun `input format has no symbol or commas`() {
        assertEquals("1250", rs.formatForInput(Money.ofRupees(1_250)))
        assertEquals("1250.05", rs.formatForInput(Money(125_005)))
    }
}
