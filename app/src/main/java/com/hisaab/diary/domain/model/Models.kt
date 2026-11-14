package com.hisaab.diary.domain.model

import com.hisaab.diary.core.money.Money

/** A khata book, e.g. "Personal" or "Shop". */
data class Book(val id: Long, val name: String, val createdAt: Long)

/** A person or business in a book. */
data class Party(
    val id: Long,
    val bookId: Long,
    val name: String,
    val phone: String?,
    val note: String?,
    val createdAt: Long,
    val isArchived: Boolean,
)

/** GAVE = you gave money (they owe you more). GOT = you got money. */
enum class EntryType { GAVE, GOT }

/** One khata entry. */
data class Entry(
    val id: Long,
    val partyId: Long,
    val type: EntryType,
    val amount: Money,
    val note: String?,
    val date: Long,
    val dueDate: Long?,
    val attachmentPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Effect on the party balance: GOT adds, GAVE subtracts. */
    val signedAmount: Money get() = if (type == EntryType.GOT) amount else -amount
}

/** Who owes whom, from your side. */
enum class BalanceStatus { WILL_GET, WILL_GIVE, SETTLED }

/**
 * Balance = total GOT − total GAVE.
 * Negative → you will get money. Positive → you will give money.
 */
fun Money.balanceStatus(): BalanceStatus = when {
    isNegative -> BalanceStatus.WILL_GET
    isPositive -> BalanceStatus.WILL_GIVE
    else -> BalanceStatus.SETTLED
}

/** A party with its totals, for lists. */
data class PartySummary(
    val party: Party,
    val totalGave: Money,
    val totalGot: Money,
    val lastEntryDate: Long?,
    val entryCount: Int,
) {
    val balance: Money get() = totalGot - totalGave
    val status: BalanceStatus get() = balance.balanceStatus()
}

/** An entry with the party balance right after it. */
data class LedgerEntry(val entry: Entry, val runningBalance: Money)

/** Totals for a book. Both values are ≥ 0. */
data class BookTotals(val willGet: Money, val willGive: Money) {
    /** Positive → overall others owe you. */
    val net: Money get() = willGet - willGive
}

/**
 * Adds a running balance to the entries of one party.
 * @param entries sorted oldest first.
 */
fun runningBalance(entries: List<Entry>): List<LedgerEntry> {
    var balance = Money.ZERO
    return entries.map { entry ->
        balance += entry.signedAmount
        LedgerEntry(entry, balance)
    }
}
