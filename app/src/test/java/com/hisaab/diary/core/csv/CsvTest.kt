package com.hisaab.diary.core.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test
    fun `plain fields are not quoted`() {
        assertEquals("a,b,\r\n", Csv.line(listOf("a", "b", null)))
    }

    @Test
    fun `special characters are quoted`() {
        assertEquals("\"Ali, Raza\"", Csv.escape("Ali, Raza"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", Csv.escape("two\nlines"))
    }

    @Test
    fun `formula-like text is made safe`() {
        assertEquals("'=SUM(A1)", Csv.escape("=SUM(A1)"))
        assertEquals("'+92300", Csv.escape("+92300"))
        assertEquals("1250.50", Csv.escape("1250.50"))
    }
}
