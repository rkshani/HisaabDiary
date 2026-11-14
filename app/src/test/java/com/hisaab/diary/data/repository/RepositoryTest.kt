package com.hisaab.diary.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.local.HisaabDatabase
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: HisaabDatabase
    private lateinit var books: BookRepositoryImpl
    private lateinit var parties: PartyRepositoryImpl
    private lateinit var entries: EntryRepositoryImpl
    private val clock = Clock { NOW }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            HisaabDatabase::class.java,
        ).allowMainThreadQueries().build()
        val io = Dispatchers.Unconfined
        books = BookRepositoryImpl(db.bookDao(), clock, io)
        parties = PartyRepositoryImpl(db.partyDao(), clock, io)
        entries = EntryRepositoryImpl(db.transactionDao(), io)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun bookAddRenameDelete() = runTest {
        val id = books.addBook("  Shop ")
        assertEquals("Shop", books.getBook(id)!!.name)
        assertEquals(NOW, books.getBook(id)!!.createdAt)

        books.renameBook(id, "Dukaan")
        assertEquals(listOf("Dukaan"), books.observeBooks().first().map { it.name })
        assertEquals(1, books.count())

        books.deleteBook(id)
        assertNull(books.getBook(id))
    }

    @Test
    fun partyCleansInputAndUpdates() = runTest {
        val bookId = books.addBook("Personal")
        val id = parties.addParty(bookId, " Ali ", phone = "  ", note = " friend ")
        val party = parties.getParty(id)!!
        assertEquals("Ali", party.name)
        assertNull(party.phone)
        assertEquals("friend", party.note)

        parties.updateParty(party.copy(phone = "0300"))
        assertEquals("0300", parties.getParty(id)!!.phone)

        parties.setArchived(id, true)
        assertEquals(0, parties.observeParties(bookId).first().size)
        assertEquals(1, parties.observeParties(bookId, archived = true).first().size)

        parties.deleteParty(id)
        assertNull(parties.getParty(id))
    }

    @Test
    fun ledgerTotalsAndUndo() = runTest {
        val bookId = books.addBook("Personal")
        val ali = parties.addParty(bookId, "Ali", null, null)
        val gave = entries.addEntry(entry(ali, EntryType.GAVE, 2_000, date = 1))
        entries.addEntry(entry(ali, EntryType.GOT, 500, date = 2))

        val ledger = entries.observeLedger(ali).first()
        assertEquals(listOf(-2_000L, -1_500L), ledger.map { it.runningBalance.paisa })
        assertEquals(Money(-1_500), parties.observeParty(ali).first()!!.balance)
        assertEquals(BookTotals(Money(1_500), Money.ZERO), books.observeTotals(bookId).first())

        val deleted = entries.getEntry(gave)!!
        entries.deleteEntry(gave)
        assertEquals(1, entries.observeLedger(ali).first().size)

        entries.restoreEntry(deleted)
        assertEquals(deleted, entries.getEntry(gave))
    }

    @Test
    fun updateEntryAndRangeQuery() = runTest {
        val bookId = books.addBook("Personal")
        val ali = parties.addParty(bookId, "Ali", null, null)
        val id = entries.addEntry(entry(ali, EntryType.GAVE, 100, date = 10))
        entries.updateEntry(entries.getEntry(id)!!.copy(amount = Money(900)))
        assertEquals(Money(900), entries.getEntry(id)!!.amount)

        assertEquals(1, entries.observeEntriesBetween(bookId, 0, 10).first().size)
        assertEquals(0, entries.observeEntriesBetween(bookId, 11, 20).first().size)
    }

    private fun entry(partyId: Long, type: EntryType, paisa: Long, date: Long) = Entry(
        id = 0,
        partyId = partyId,
        type = type,
        amount = Money(paisa),
        note = null,
        date = date,
        dueDate = null,
        attachmentPath = null,
        createdAt = NOW,
        updatedAt = NOW,
    )

    private companion object {
        const val NOW = 1_000L
    }
}
