package com.hisaab.diary.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.data.local.HisaabDatabase
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType
import com.hisaab.diary.data.local.entity.TransactionType.GAVE
import com.hisaab.diary.data.local.entity.TransactionType.GOT
import com.hisaab.diary.data.local.model.BookTotals
import com.hisaab.diary.data.local.model.PartyWithBalance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: HisaabDatabase
    private lateinit var books: BookDao
    private lateinit var parties: PartyDao
    private lateinit var entries: TransactionDao
    private var bookId = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            HisaabDatabase::class.java,
        ).allowMainThreadQueries().build()
        books = db.bookDao()
        parties = db.partyDao()
        entries = db.transactionDao()
        bookId = books.insert(BookEntity(name = "Personal", createdAt = 0))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun partyBalanceIsGotMinusGave() = runTest {
        val ali = addParty("Ali")
        addEntry(ali, GAVE, 200_000, date = 1)
        addEntry(ali, GOT, 50_000, date = 2)

        val row = parties.observeWithBalance(bookId).first().single()
        assertEquals(200_000L, row.totalGavePaisa)
        assertEquals(50_000L, row.totalGotPaisa)
        assertEquals(-150_000L, row.balancePaisa) // Ali owes you Rs 1,500
        assertEquals(2L, row.lastEntryDate)
        assertEquals(2, row.entryCount)
    }

    @Test
    fun partyWithoutEntriesHasZeroBalance() = runTest {
        addParty("New")
        val row = parties.observeWithBalance(bookId).first().single()
        assertEquals(0L, row.balancePaisa)
        assertNull(row.lastEntryDate)
        assertEquals(0, row.entryCount)
    }

    @Test
    fun searchByNameOrPhone() = runTest {
        addParty("Ali Raza", phone = "03001234567")
        addParty("Bilal", phone = "03219999999")

        assertEquals(listOf("Ali Raza"), parties.observeWithBalance(bookId, "ali").first().names())
        assertEquals(listOf("Bilal"), parties.observeWithBalance(bookId, "0321").first().names())
        assertEquals(2, parties.observeWithBalance(bookId, "").first().size)
    }

    @Test
    fun archivedPartiesAreHiddenByDefault() = runTest {
        val old = addParty("Old")
        addParty("Active")
        parties.setArchived(old, true)

        assertEquals(listOf("Active"), parties.observeWithBalance(bookId).first().names())
        assertEquals(listOf("Old"), parties.observeWithBalance(bookId, archived = true).first().names())
    }

    @Test
    fun partiesAreSortedByLatestActivity() = runTest {
        val a = addParty("A", createdAt = 1)
        addParty("B", createdAt = 5)
        addEntry(a, GAVE, 100, date = 10)

        assertEquals(listOf("A", "B"), parties.observeWithBalance(bookId).first().names())
    }

    @Test
    fun bookTotalsCountEachPartyOnOneSide() = runTest {
        val ali = addParty("Ali")
        addEntry(ali, GAVE, 1_000)
        addEntry(ali, GOT, 300) // Ali owes 700
        val bilal = addParty("Bilal")
        addEntry(bilal, GOT, 500) // you owe Bilal 500
        val settled = addParty("Settled")
        addEntry(settled, GAVE, 200)
        addEntry(settled, GOT, 200)
        addParty("Empty")

        assertEquals(BookTotals(willGetPaisa = 700, willGivePaisa = 500), books.observeTotals(bookId).first())
    }

    @Test
    fun bookTotalsOfEmptyBookAreZero() = runTest {
        assertEquals(BookTotals(0, 0), books.observeTotals(bookId).first())
    }

    @Test
    fun bookTotalsIgnoreOtherBooksAndArchived() = runTest {
        val other = books.insert(BookEntity(name = "Shop", createdAt = 0))
        val shopParty = parties.insert(PartyEntity(bookId = other, name = "X", createdAt = 0))
        addEntry(shopParty, GAVE, 999)
        val archived = addParty("Gone")
        addEntry(archived, GAVE, 111)
        parties.setArchived(archived, true)

        assertEquals(BookTotals(0, 0), books.observeTotals(bookId).first())
        assertEquals(BookTotals(999, 0), books.observeTotals(other).first())
    }

    @Test
    fun ledgerIsOldestFirstAndUpdatesLive() = runTest {
        val ali = addParty("Ali")
        addEntry(ali, GAVE, 100, date = 20)
        addEntry(ali, GOT, 50, date = 10)

        entries.observeByParty(ali).test {
            assertEquals(listOf(10L, 20L), awaitItem().map { it.date })
            addEntry(ali, GAVE, 5, date = 15)
            assertEquals(listOf(10L, 15L, 20L), awaitItem().map { it.date })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun entriesBetweenDatesForReports() = runTest {
        val ali = addParty("Ali")
        addEntry(ali, GAVE, 1, date = 5)
        addEntry(ali, GAVE, 2, date = 10)
        addEntry(ali, GAVE, 3, date = 20)

        val result = entries.observeByBookBetween(bookId, from = 5, to = 10).first()
        assertEquals(listOf(10L, 5L), result.map { it.date })
    }

    @Test
    fun updateAndDeleteEntry() = runTest {
        val ali = addParty("Ali")
        val id = addEntry(ali, GAVE, 100)
        val entry = entries.getById(id)!!
        entries.update(entry.copy(amountPaisa = 250, updatedAt = 9))
        assertEquals(250L, entries.getById(id)!!.amountPaisa)

        entries.delete(entries.getById(id)!!)
        assertNull(entries.getById(id))
    }

    @Test
    fun observeOnePartyWithBalance() = runTest {
        val ali = addParty("Ali")
        addEntry(ali, GOT, 300)
        assertEquals(300L, parties.observeOneWithBalance(ali).first()!!.balancePaisa)
        assertNull(parties.observeOneWithBalance(999).first())
    }

    private suspend fun addParty(name: String, phone: String? = null, createdAt: Long = 0): Long =
        parties.insert(PartyEntity(bookId = bookId, name = name, phone = phone, createdAt = createdAt))

    private suspend fun addEntry(partyId: Long, type: TransactionType, paisa: Long, date: Long = 0): Long =
        entries.insert(
            TransactionEntity(
                partyId = partyId,
                type = type,
                amountPaisa = paisa,
                date = date,
                createdAt = 0,
                updatedAt = 0,
            ),
        )

    private fun List<PartyWithBalance>.names() = map { it.party.name }
}
