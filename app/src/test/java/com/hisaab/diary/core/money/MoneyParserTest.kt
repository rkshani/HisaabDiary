package com.hisaab.diary.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParserTest {
    @Test
    fun `parses plain and decimal numbers`() {
        assertEquals(Money(125_000), MoneyParser.parse("1250"))
        assertEquals(Money(125_050), MoneyParser.parse("1250.5"))
        assertEquals(Money(125_055), MoneyParser.parse("1250.55"))
        assertEquals(Money(125_000), MoneyParser.parse("1250."))
        assertEquals(Money(50), MoneyParser.parse(".5"))
        assertEquals(Money.ZERO, MoneyParser.parse("0"))
    }

    @Test
    fun `ignores commas spaces and symbol`() {
        assertEquals(Money(125_000), MoneyParser.parse(" 1,250 "))
        assertEquals(Money(9_999), MoneyParser.parse("Rs 99.99"))
        assertEquals(Money(9_999), MoneyParser.parse("₨99.99"))
    }

    @Test
    fun `rejects bad input`() {
        listOf("", " ", "abc", "-5", "12.345", "1.2.3", "12a", ".", "Rs").forEach {
            assertNull("should reject [$it]", MoneyParser.parse(it))
        }
    }

    @Test
    fun `rejects too big amounts`() {
        assertEquals(MoneyParser.MAX, MoneyParser.parse("999999999999.99"))
        assertNull(MoneyParser.parse("1000000000000"))
        assertNull(MoneyParser.parse("99999999999999999999"))
    }
}
