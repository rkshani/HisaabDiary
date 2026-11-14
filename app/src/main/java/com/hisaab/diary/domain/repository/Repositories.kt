package com.hisaab.diary.domain.repository

import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.ExportRow
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>

    fun observeTotals(bookId: Long): Flow<BookTotals>

    suspend fun getBook(id: Long): Book?

    suspend fun count(): Int

    /** @return the new book id. */
    suspend fun addBook(name: String): Long

    suspend fun renameBook(id: Long, name: String)

    /** Deletes the book with all its parties and entries. */
    suspend fun deleteBook(id: Long)
}

interface PartyRepository {
    /** Parties of a book with totals. [query] matches name or phone. */
    fun observeParties(bookId: Long, query: String = "", archived: Boolean = false): Flow<List<PartySummary>>

    fun observeParty(id: Long): Flow<PartySummary?>

    suspend fun getParty(id: Long): Party?

    /** @return the new party id. */
    suspend fun addParty(bookId: Long, name: String, phone: String?, note: String?): Long

    suspend fun updateParty(party: Party)

    suspend fun setArchived(id: Long, archived: Boolean)

    /** Deletes the party with all its entries. */
    suspend fun deleteParty(id: Long)
}

interface EntryRepository {
    /** Entries of a party, oldest first, with running balance. */
    fun observeLedger(partyId: Long): Flow<List<LedgerEntry>>

    /** Entries of a book between two dates (inclusive), newest first. */
    fun observeEntriesBetween(bookId: Long, from: Long, to: Long): Flow<List<Entry>>

    suspend fun getEntry(id: Long): Entry?

    /** Saves a new entry ([Entry.id] is ignored). @return the new id. */
    suspend fun addEntry(entry: Entry): Long

    suspend fun updateEntry(entry: Entry)

    suspend fun deleteEntry(id: Long)

    /** Entries with names for export; all books when [bookId] is null. Oldest first. */
    suspend fun getForExport(bookId: Long?): List<ExportRow>

    /** Puts back a deleted entry with its old id (for Undo). */
    suspend fun restoreEntry(entry: Entry)
}
