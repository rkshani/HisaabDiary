package com.hisaab.diary.domain.model

import com.hisaab.diary.core.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun `running balance adds GOT and subtracts GAVE`() {
        val ledger = runningBalance(
            listOf(entry(EntryType.GAVE, 1_000), entry(EntryType.GOT, 300), entry(EntryType.GOT, 900)),
        )
        assertEquals(listOf(-1_000L, -700L, 200L), ledger.map { it.runningBalance.paisa })
        assertTrue(runningBalance(emptyList()).isEmpty())
    }

    @Test
    fun `balance status from sign`() {
        assertEquals(BalanceStatus.WILL_GET, Money(-1).balanceStatus())
        assertEquals(BalanceStatus.WILL_GIVE, Money(1).balanceStatus())
        assertEquals(BalanceStatus.SETTLED, Money.ZERO.balanceStatus())
    }

    @Test
    fun `party summary balance`() {
        val summary = PartySummary(
            party = Party(1, 1, "Ali", null, null, 0, false),
            totalGave = Money(500),
            totalGot = Money(200),
            lastEntryDate = null,
            entryCount = 2,
        )
        assertEquals(Money(-300), summary.balance)
        assertEquals(BalanceStatus.WILL_GET, summary.status)
    }

    @Test
    fun `book net is get minus give`() {
        assertEquals(Money(300), BookTotals(willGet = Money(500), willGive = Money(200)).net)
    }

    private fun entry(type: EntryType, paisa: Long) = Entry(
        id = 0,
        partyId = 1,
        type = type,
        amount = Money(paisa),
        note = null,
        date = 0,
        dueDate = null,
        attachmentPath = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
