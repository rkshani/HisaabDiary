package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettleUpUseCaseTest {
    private val store = FakeStore()
    private val parties = FakePartyRepository(store)
    private val entries = FakeEntryRepository(store)
    private val clock = Clock { 500 }
    private val settleUp = SettleUpUseCase(parties, AddTransactionUseCase(entries, clock), clock)

    @Test
    fun `they owe you - adds a GOT entry`() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        entries.addEntry(entry(ali, EntryType.GAVE, 1_500))
        entries.addEntry(entry(ali, EntryType.GOT, 500))

        val id = settleUp(ali, "Settled")!!
        val added = entries.getEntry(id)!!
        assertEquals(EntryType.GOT, added.type)
        assertEquals(Money(1_000), added.amount)
        assertEquals("Settled", added.note)
        assertEquals(500L, added.date)
        assertEquals(Money.ZERO, parties.observeParty(ali).first()!!.balance)
    }

    @Test
    fun `you owe them - adds a GAVE entry`() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        entries.addEntry(entry(ali, EntryType.GOT, 700))

        val added = entries.getEntry(settleUp(ali, null)!!)!!
        assertEquals(EntryType.GAVE, added.type)
        assertEquals(Money(700), added.amount)
    }

    @Test
    fun `settled or missing party does nothing`() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        assertNull(settleUp(ali, null))
        assertNull(settleUp(999, null))
        assertEquals(0, store.entries.value.size)
    }

    private fun entry(partyId: Long, type: EntryType, paisa: Long) =
        Entry(0, partyId, type, Money(paisa), null, 0, null, null, 0, 0)
}
