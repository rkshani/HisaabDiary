package com.hisaab.diary.domain.model

import com.hisaab.diary.core.money.Money

/** An open reminder to collect or pay money to one party. */
data class Reminder(
    val id: Long,
    val partyId: Long,
    val bookId: Long,
    val partyName: String,
    val remindAt: Long,
    /** Party balance now (GOT − GAVE). Negative → you will get. */
    val balance: Money,
) {
    val status: BalanceStatus get() = balance.balanceStatus()
}
