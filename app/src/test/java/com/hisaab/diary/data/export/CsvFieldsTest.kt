package com.hisaab.diary.data.export

import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.ExportRow
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CsvFieldsTest {
    private lateinit var oldZone: TimeZone

    @Before
    fun setUp() {
        oldZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() = TimeZone.setDefault(oldZone)

    @Test
    fun `fields in column order`() {
        val entry = Entry(1, 1, EntryType.GAVE, Money(125_050), "chai", 0, DAY, null, 0, 0)
        val fields = csvFields(ExportRow(entry, "Personal", "Ali", "0300"), typeLabel = "You gave")
        assertEquals(
            listOf("1970-01-01", "Personal", "Ali", "0300", "You gave", "1250.50", "chai", "1970-01-02"),
            fields,
        )
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
