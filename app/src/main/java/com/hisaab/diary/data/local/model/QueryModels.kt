package com.hisaab.diary.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType

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

/** An entry plus the party balance right after it (same sign rule as [PartyWithBalance]). */
data class LedgerRow(val entry: TransactionEntity, val runningBalancePaisa: Long)

/**
 * Adds a running balance to entries of ONE party.
 * Done in Kotlin because SQL window functions need SQLite 3.25 (Android 11+), and minSdk is 24.
 * @param entries sorted oldest first.
 */
fun withRunningBalance(entries: List<TransactionEntity>): List<LedgerRow> {
    var balance = 0L
    return entries.map { entry ->
        balance += when (entry.type) {
            TransactionType.GOT -> entry.amountPaisa
            TransactionType.GAVE -> -entry.amountPaisa
        }
        LedgerRow(entry, balance)
    }
}
