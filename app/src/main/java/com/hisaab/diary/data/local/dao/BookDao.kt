package com.hisaab.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.model.BookTotals
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY created_at, id")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    /**
     * Sum of what others owe you ([BookTotals.willGetPaisa]) and what you owe ([BookTotals.willGivePaisa]),
     * over all non-archived parties of the book. Each party counts only on one side.
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN balance < 0 THEN -balance ELSE 0 END), 0) AS will_get,
            COALESCE(SUM(CASE WHEN balance > 0 THEN balance ELSE 0 END), 0) AS will_give
        FROM (
            SELECT COALESCE(SUM(CASE t.type WHEN 'GOT' THEN t.amount_paisa ELSE -t.amount_paisa END), 0) AS balance
            FROM parties p
            LEFT JOIN transactions t ON t.party_id = p.id
            WHERE p.book_id = :bookId AND p.is_archived = 0
            GROUP BY p.id
        )
        """,
    )
    fun observeTotals(bookId: Long): Flow<BookTotals>
}
