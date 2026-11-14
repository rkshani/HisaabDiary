package com.hisaab.diary.data.repository

import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.data.local.dao.BookDao
import com.hisaab.diary.data.local.dao.PartyDao
import com.hisaab.diary.data.local.dao.TransactionDao
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.mapper.toDomain
import com.hisaab.diary.data.mapper.toEntity
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.model.runningBalance
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.EntryRepository
import com.hisaab.diary.domain.repository.PartyRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookRepositoryImpl @Inject constructor(
    private val dao: BookDao,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTotals(bookId: Long): Flow<BookTotals> = dao.observeTotals(bookId).map { it.toDomain() }

    override suspend fun getBook(id: Long): Book? = withContext(io) { dao.getById(id)?.toDomain() }

    override suspend fun count(): Int = withContext(io) { dao.count() }

    override suspend fun addBook(name: String): Long = withContext(io) {
        dao.insert(BookEntity(name = name.trim(), createdAt = clock.nowMillis()))
    }

    override suspend fun renameBook(id: Long, name: String) = withContext(io) {
        dao.getById(id)?.let { dao.update(it.copy(name = name.trim())) } ?: Unit
    }

    override suspend fun deleteBook(id: Long) = withContext(io) {
        dao.getById(id)?.let { dao.delete(it) } ?: Unit
    }
}

class PartyRepositoryImpl @Inject constructor(
    private val dao: PartyDao,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PartyRepository {
    override fun observeParties(bookId: Long, query: String, archived: Boolean): Flow<List<PartySummary>> =
        dao.observeWithBalance(bookId, query.trim(), archived).map { list -> list.map { it.toDomain() } }

    override fun observeParty(id: Long): Flow<PartySummary?> = dao.observeOneWithBalance(id).map { it?.toDomain() }

    override suspend fun getParty(id: Long): Party? = withContext(io) { dao.getById(id)?.toDomain() }

    override suspend fun addParty(bookId: Long, name: String, phone: String?, note: String?): Long = withContext(io) {
        dao.insert(
            PartyEntity(
                bookId = bookId,
                name = name.trim(),
                phone = phone?.trim()?.ifEmpty { null },
                note = note?.trim()?.ifEmpty { null },
                createdAt = clock.nowMillis(),
            ),
        )
    }

    override suspend fun updateParty(party: Party) = withContext(io) { dao.update(party.toEntity()) }

    override suspend fun setArchived(id: Long, archived: Boolean) = withContext(io) { dao.setArchived(id, archived) }

    override suspend fun deleteParty(id: Long) = withContext(io) {
        dao.getById(id)?.let { dao.delete(it) } ?: Unit
    }
}

class EntryRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : EntryRepository {
    override fun observeLedger(partyId: Long): Flow<List<LedgerEntry>> = dao.observeByParty(partyId)
        .map { list -> runningBalance(list.map { it.toDomain() }) }
        .flowOn(io)

    override fun observeEntriesBetween(bookId: Long, from: Long, to: Long): Flow<List<Entry>> =
        dao.observeByBookBetween(bookId, from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): Entry? = withContext(io) { dao.getById(id)?.toDomain() }

    override suspend fun addEntry(entry: Entry): Long = withContext(io) { dao.insert(entry.copy(id = 0).toEntity()) }

    override suspend fun updateEntry(entry: Entry) = withContext(io) { dao.update(entry.toEntity()) }

    override suspend fun deleteEntry(id: Long) = withContext(io) {
        dao.getById(id)?.let { dao.delete(it) } ?: Unit
    }

    override suspend fun restoreEntry(entry: Entry) = withContext(io) {
        dao.insert(entry.toEntity())
        Unit
    }
}
