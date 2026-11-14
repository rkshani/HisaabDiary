package com.hisaab.diary.domain.fake

import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.ExportRow
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.model.runningBalance
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.EntryRepository
import com.hisaab.diary.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory repositories for unit tests. They share one [entries] store. */
class FakeStore {
    val books = MutableStateFlow<List<Book>>(emptyList())
    val parties = MutableStateFlow<List<Party>>(emptyList())
    val entries = MutableStateFlow<List<Entry>>(emptyList())
    private var nextId = 1L

    fun newId() = nextId++

    fun summary(party: Party, all: List<Entry>): PartySummary {
        val mine = all.filter { it.partyId == party.id }
        return PartySummary(
            party = party,
            totalGave = Money(mine.filter { it.type == EntryType.GAVE }.sumOf { it.amount.paisa }),
            totalGot = Money(mine.filter { it.type == EntryType.GOT }.sumOf { it.amount.paisa }),
            lastEntryDate = mine.maxOfOrNull { it.date },
            entryCount = mine.size,
        )
    }
}

class FakeBookRepository(private val store: FakeStore) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = store.books

    override fun observeTotals(bookId: Long): Flow<BookTotals> = combine(store.parties, store.entries) { parties, all ->
        val balances = parties.filter { it.bookId == bookId && !it.isArchived }.map { store.summary(it, all).balance }
        BookTotals(
            willGet = Money(balances.filter { it.isNegative }.sumOf { -it.paisa }),
            willGive = Money(balances.filter { it.isPositive }.sumOf { it.paisa }),
        )
    }

    override suspend fun getBook(id: Long) = store.books.value.find { it.id == id }

    override suspend fun count() = store.books.value.size

    override suspend fun addBook(name: String): Long = store.newId().also { id ->
        store.books.update { it + Book(id, name, 0) }
    }

    override suspend fun renameBook(id: Long, name: String) =
        store.books.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }

    override suspend fun deleteBook(id: Long) = store.books.update { list -> list.filterNot { it.id == id } }
}

class FakePartyRepository(private val store: FakeStore) : PartyRepository {
    override fun observeParties(bookId: Long, query: String, archived: Boolean): Flow<List<PartySummary>> =
        combine(store.parties, store.entries) { parties, all ->
            parties.filter { it.bookId == bookId && it.isArchived == archived && it.name.contains(query, true) }
                .map { store.summary(it, all) }
        }

    override fun observeParty(id: Long): Flow<PartySummary?> = combine(store.parties, store.entries) { parties, all ->
        parties.find { it.id == id }?.let { store.summary(it, all) }
    }

    override suspend fun getParty(id: Long) = store.parties.value.find { it.id == id }

    override suspend fun addParty(bookId: Long, name: String, phone: String?, note: String?): Long =
        store.newId().also { id ->
            store.parties.update {
                it +
                    Party(id, bookId, name, phone?.ifEmpty { null }, note?.ifEmpty { null }, 0, false)
            }
        }

    override suspend fun updateParty(party: Party) =
        store.parties.update { list -> list.map { if (it.id == party.id) party else it } }

    override suspend fun setArchived(id: Long, archived: Boolean) =
        store.parties.update { list -> list.map { if (it.id == id) it.copy(isArchived = archived) else it } }

    override suspend fun deleteParty(id: Long) {
        store.parties.update { list -> list.filterNot { it.id == id } }
        store.entries.update { list -> list.filterNot { it.partyId == id } }
    }
}

class FakeEntryRepository(private val store: FakeStore) : EntryRepository {
    override fun observeLedger(partyId: Long): Flow<List<LedgerEntry>> = store.entries.map { all ->
        runningBalance(all.filter { it.partyId == partyId }.sortedWith(compareBy({ it.date }, { it.id })))
    }

    override fun observeEntriesBetween(bookId: Long, from: Long, to: Long): Flow<List<Entry>> =
        store.entries.map { all -> all.filter { it.date in from..to }.sortedByDescending { it.date } }

    override suspend fun getEntry(id: Long) = store.entries.value.find { it.id == id }

    override suspend fun addEntry(entry: Entry): Long = store.newId().also { id ->
        store.entries.update { it + entry.copy(id = id) }
    }

    override suspend fun updateEntry(entry: Entry) =
        store.entries.update { list -> list.map { if (it.id == entry.id) entry else it } }

    override suspend fun deleteEntry(id: Long) = store.entries.update { list -> list.filterNot { it.id == id } }

    override suspend fun restoreEntry(entry: Entry) = store.entries.update { it + entry }

    override suspend fun getForExport(bookId: Long?): List<ExportRow> = store.entries.value
        .sortedWith(compareBy({ it.date }, { it.id }))
        .mapNotNull { entry ->
            val party = store.parties.value.find { it.id == entry.partyId } ?: return@mapNotNull null
            if (bookId != null && party.bookId != bookId) return@mapNotNull null
            val book = store.books.value.find { it.id == party.bookId }?.name.orEmpty()
            ExportRow(entry, book, party.name, party.phone)
        }
}
