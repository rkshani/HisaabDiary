package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.repository.PartyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Adds one entry that brings the party balance to zero.
 * They owe you → "You got" the full amount. You owe them → "You gave" it.
 * @return the new entry id, or null when the balance is already zero or the party is gone.
 */
class SettleUpUseCase @Inject constructor(
    private val parties: PartyRepository,
    private val addTransaction: AddTransactionUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(partyId: Long, note: String?): Long? {
        val summary = parties.observeParty(partyId).first() ?: return null
        val type = when (summary.status) {
            BalanceStatus.WILL_GET -> EntryType.GOT
            BalanceStatus.WILL_GIVE -> EntryType.GAVE
            BalanceStatus.SETTLED -> return null
        }
        val input = EntryInput(
            partyId = partyId,
            type = type,
            amount = summary.balance.abs(),
            note = note,
            date = clock.nowMillis(),
            dueDate = null,
        )
        return (addTransaction(input) as? Outcome.Success)?.value
    }
}
