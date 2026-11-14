package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import org.junit.Assert.assertEquals
import org.junit.Test

class PartyListOptionsTest {
    // Input order = "recent" order from the database.
    private val list = listOf(
        summary(1, "zara", gave = 100, got = 0), // will get 100
        summary(2, "Ali", gave = 0, got = 500), // will give 500
        summary(3, "Bilal", gave = 50, got = 50), // settled
        summary(4, "imran", gave = 900, got = 0), // will get 900
    )

    @Test
    fun `recent keeps database order`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), list.applyOptions(PartySort.RECENT, PartyFilter.ALL).ids())
    }

    @Test
    fun `name sort ignores case`() {
        assertEquals(listOf(2L, 3L, 4L, 1L), list.applyOptions(PartySort.NAME, PartyFilter.ALL).ids())
    }

    @Test
    fun `highest amount sorts by absolute balance`() {
        assertEquals(listOf(4L, 2L, 1L, 3L), list.applyOptions(PartySort.HIGHEST_AMOUNT, PartyFilter.ALL).ids())
    }

    @Test
    fun filters() {
        assertEquals(listOf(1L, 4L), list.applyOptions(PartySort.RECENT, PartyFilter.WILL_GET).ids())
        assertEquals(listOf(2L), list.applyOptions(PartySort.RECENT, PartyFilter.WILL_GIVE).ids())
        assertEquals(listOf(3L), list.applyOptions(PartySort.RECENT, PartyFilter.SETTLED).ids())
    }

    @Test
    fun `filter and sort together`() {
        assertEquals(listOf(4L, 1L), list.applyOptions(PartySort.HIGHEST_AMOUNT, PartyFilter.WILL_GET).ids())
    }

    private fun List<PartySummary>.ids() = map { it.party.id }

    private fun summary(id: Long, name: String, gave: Long, got: Long) = PartySummary(
        party = Party(id, 1, name, null, null, 0, false),
        totalGave = Money(gave),
        totalGot = Money(got),
        lastEntryDate = null,
        entryCount = 0,
    )
}
