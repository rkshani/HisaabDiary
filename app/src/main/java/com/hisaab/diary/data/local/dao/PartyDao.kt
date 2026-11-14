package com.hisaab.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.model.PartyWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    /**
     * Parties of a book with their totals. [query] matches name or phone (empty = all).
     * Sorted by the newest activity first; the UI can re-sort.
     */
    @Query(
        """
        SELECT p.*,
            COALESCE(SUM(CASE WHEN t.type = 'GAVE' THEN t.amount_paisa ELSE 0 END), 0) AS total_gave,
            COALESCE(SUM(CASE WHEN t.type = 'GOT' THEN t.amount_paisa ELSE 0 END), 0) AS total_got,
            MAX(t.date) AS last_entry_date,
            COUNT(t.id) AS entry_count
        FROM parties p
        LEFT JOIN transactions t ON t.party_id = p.id
        WHERE p.book_id = :bookId
            AND p.is_archived = :archived
            AND (:query = '' OR p.name LIKE '%' || :query || '%' OR p.phone LIKE '%' || :query || '%')
        GROUP BY p.id
        ORDER BY COALESCE(MAX(t.date), p.created_at) DESC, p.id DESC
        """,
    )
    fun observeWithBalance(bookId: Long, query: String = "", archived: Boolean = false): Flow<List<PartyWithBalance>>

    @Query(
        """
        SELECT p.*,
            COALESCE(SUM(CASE WHEN t.type = 'GAVE' THEN t.amount_paisa ELSE 0 END), 0) AS total_gave,
            COALESCE(SUM(CASE WHEN t.type = 'GOT' THEN t.amount_paisa ELSE 0 END), 0) AS total_got,
            MAX(t.date) AS last_entry_date,
            COUNT(t.id) AS entry_count
        FROM parties p
        LEFT JOIN transactions t ON t.party_id = p.id
        WHERE p.id = :partyId
        GROUP BY p.id
        """,
    )
    fun observeOneWithBalance(partyId: Long): Flow<PartyWithBalance?>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: Long): PartyEntity?

    @Insert
    suspend fun insert(party: PartyEntity): Long

    @Update
    suspend fun update(party: PartyEntity)

    @Delete
    suspend fun delete(party: PartyEntity)

    @Query("UPDATE parties SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}
