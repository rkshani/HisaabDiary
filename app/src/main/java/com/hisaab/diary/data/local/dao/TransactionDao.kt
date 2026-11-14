package com.hisaab.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.model.ExportRowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** All entries of one party, oldest first (so a running balance can be added). */
    @Query("SELECT * FROM transactions WHERE party_id = :partyId ORDER BY date, id")
    fun observeByParty(partyId: Long): Flow<List<TransactionEntity>>

    /** Entries of a book between [from] and [to] (inclusive), newest first. For reports. */
    @Query(
        """
        SELECT t.* FROM transactions t
        INNER JOIN parties p ON p.id = t.party_id
        WHERE p.book_id = :bookId AND t.date BETWEEN :from AND :to
        ORDER BY t.date DESC, t.id DESC
        """,
    )
    fun observeByBookBetween(bookId: Long, from: Long, to: Long): Flow<List<TransactionEntity>>

    /** All entries (of one book, or all books when [bookId] is null) with names, oldest first. */
    @Query(
        """
        SELECT t.*, p.name AS party_name, p.phone AS party_phone, b.name AS book_name
        FROM transactions t
        INNER JOIN parties p ON p.id = t.party_id
        INNER JOIN books b ON b.id = p.book_id
        WHERE (:bookId IS NULL OR p.book_id = :bookId)
        ORDER BY t.date, t.id
        """,
    )
    suspend fun getForExport(bookId: Long?): List<ExportRowEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(entry: TransactionEntity): Long

    @Update
    suspend fun update(entry: TransactionEntity)

    @Delete
    suspend fun delete(entry: TransactionEntity)
}
