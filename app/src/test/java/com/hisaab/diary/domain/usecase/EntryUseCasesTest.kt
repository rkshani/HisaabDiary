package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.core.money.MoneyParser
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryUseCasesTest {
    private val store = FakeStore()
    private val entries = FakeEntryRepository(store)
    private var now = 100L
    private val clock = Clock { now }
    private val add = AddTransactionUseCase(entries, clock)
    private val update = UpdateTransactionUseCase(entries, clock)
    private val delete = DeleteTransactionUseCase(entries)
    private val undo = UndoDeleteTransactionUseCase(entries)

    @Test
    fun `add entry sets timestamps and trims note`() = runTest {
        val id = (add(input(note = "  chai  ")) as Outcome.Success).value
        val entry = entries.getEntry(id)!!
        assertEquals("chai", entry.note)
        assertEquals(100L, entry.createdAt)
        assertEquals(100L, entry.updatedAt)
    }

    @Test
    fun `blank note becomes null`() = runTest {
        val id = (add(input(note = "   ")) as Outcome.Success).value
        assertNull(entries.getEntry(id)!!.note)
    }

    @Test
    fun `add entry validates`() = runTest {
        assertEquals(Outcome.Failure(EntryError.AMOUNT_NOT_POSITIVE), add(input(amount = Money.ZERO)))
        assertEquals(Outcome.Failure(EntryError.AMOUNT_NOT_POSITIVE), add(input(amount = Money(-5))))
        assertEquals(Outcome.Failure(EntryError.AMOUNT_TOO_BIG), add(input(amount = MoneyParser.MAX + Money(1))))
        assertEquals(Outcome.Failure(EntryError.DUE_BEFORE_DATE), add(input(date = 10, dueDate = 5)))
        assertEquals(Outcome.Failure(EntryError.NOTE_TOO_LONG), add(input(note = "x".repeat(201))))
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun `update keeps createdAt and changes updatedAt`() = runTest {
        val id = (add(input()) as Outcome.Success).value
        now = 500
        assertEquals(Outcome.Success(Unit), update(id, input(amount = Money(999), type = EntryType.GOT)))
        val entry = entries.getEntry(id)!!
        assertEquals(Money(999), entry.amount)
        assertEquals(EntryType.GOT, entry.type)
        assertEquals(100L, entry.createdAt)
        assertEquals(500L, entry.updatedAt)
    }

    @Test
    fun `update validates and needs an existing entry`() = runTest {
        assertEquals(Outcome.Failure(EntryError.NOT_FOUND), update(42, input()))
        val id = (add(input()) as Outcome.Success).value
        assertEquals(Outcome.Failure(EntryError.AMOUNT_NOT_POSITIVE), update(id, input(amount = Money.ZERO)))
    }

    @Test
    fun `delete returns entry and undo restores it`() = runTest {
        val id = (add(input()) as Outcome.Success).value
        val deleted = delete(id)!!
        assertNull(entries.getEntry(id))

        undo(deleted)
        assertEquals(deleted, entries.getEntry(id))
        assertNull(delete(999))
    }

    @Test
    fun `ledger and book totals`() = runTest {
        val parties = FakePartyRepository(store)
        val books = FakeBookRepository(store)
        val bookId = books.addBook("Personal")
        val ali = parties.addParty(bookId, "Ali", null, null)
        add(input(partyId = ali, amount = Money(1_000), type = EntryType.GAVE, date = 1))
        add(input(partyId = ali, amount = Money(400), type = EntryType.GOT, date = 2))

        val ledger = ObserveLedgerUseCase(entries)(ali).first()
        assertEquals(listOf(-1_000L, -600L), ledger.map { it.runningBalance.paisa })
        assertEquals(BookTotals(Money(600), Money.ZERO), ObserveBookTotalsUseCase(books)(bookId).first())
        assertEquals(Money(400), GetTransactionUseCase(entries)(ledger[1].entry.id)!!.amount)
    }

    private fun input(
        partyId: Long = 1,
        type: EntryType = EntryType.GAVE,
        amount: Money = Money(10_000),
        note: String? = null,
        date: Long = 50,
        dueDate: Long? = null,
    ) = EntryInput(partyId, type, amount, note, date, dueDate)
}
