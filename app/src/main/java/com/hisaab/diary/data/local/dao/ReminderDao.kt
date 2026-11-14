package com.hisaab.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hisaab.diary.data.local.entity.ReminderEntity
import com.hisaab.diary.data.local.model.ReminderWithParty
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    /** Open reminders of non-archived parties, soonest first, with the party balance (GOT − GAVE). */
    @Query(
        """
        SELECT r.id, r.party_id, r.remind_at, p.name AS party_name, p.book_id AS book_id,
            COALESCE((
                SELECT SUM(CASE t.type WHEN 'GOT' THEN t.amount_paisa ELSE -t.amount_paisa END)
                FROM transactions t WHERE t.party_id = p.id
            ), 0) AS balance
        FROM reminders r
        INNER JOIN parties p ON p.id = r.party_id
        WHERE r.is_done = 0 AND p.is_archived = 0
        ORDER BY r.remind_at, r.id
        """,
    )
    fun observeOpen(): Flow<List<ReminderWithParty>>

    @Query("SELECT * FROM reminders WHERE party_id = :partyId AND is_done = 0 LIMIT 1")
    suspend fun getOpenForParty(partyId: Long): ReminderEntity?

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET remind_at = :remindAt WHERE id = :id")
    suspend fun setRemindAt(id: Long, remindAt: Long)

    @Query("UPDATE reminders SET is_done = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    /** Closes reminders of parties that are settled now (nothing left to collect or pay). */
    @Query(
        """
        UPDATE reminders SET is_done = 1
        WHERE is_done = 0 AND party_id IN (
            SELECT p.id FROM parties p
            WHERE COALESCE((
                SELECT SUM(CASE t.type WHEN 'GOT' THEN t.amount_paisa ELSE -t.amount_paisa END)
                FROM transactions t WHERE t.party_id = p.id
            ), 0) = 0
        )
        """,
    )
    suspend fun closeSettled()
}
