package com.hisaab.diary.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.hisaab.diary.data.local.entity.PartyEntity

/**
 * A party with its totals.
 * [balancePaisa] = total GOT − total GAVE:
 * negative → you will get money, positive → you will give money, 0 → settled.
 */
data class PartyWithBalance(
    @Embedded val party: PartyEntity,
    @ColumnInfo(name = "total_gave") val totalGavePaisa: Long,
    @ColumnInfo(name = "total_got") val totalGotPaisa: Long,
    @ColumnInfo(name = "last_entry_date") val lastEntryDate: Long?,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
) {
    val balancePaisa: Long get() = totalGotPaisa - totalGavePaisa
}

/** Totals for one book: what others owe you, and what you owe others (both ≥ 0). */
data class BookTotals(
    @ColumnInfo(name = "will_get") val willGetPaisa: Long,
    @ColumnInfo(name = "will_give") val willGivePaisa: Long,
)

/** An open reminder with its party name and current balance (GOT − GAVE, in paisa). */
data class ReminderWithParty(
    val id: Long,
    @ColumnInfo(name = "party_id") val partyId: Long,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "party_name") val partyName: String,
    @ColumnInfo(name = "remind_at") val remindAt: Long,
    val balance: Long,
)
