package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.PartySummary

enum class PartySort { RECENT, NAME, HIGHEST_AMOUNT }

enum class PartyFilter { ALL, WILL_GET, WILL_GIVE, SETTLED }

/**
 * Filters and sorts a party list for Home.
 * [PartySort.RECENT] keeps the input order (the database already sorts by latest activity).
 */
fun List<PartySummary>.applyOptions(sort: PartySort, filter: PartyFilter): List<PartySummary> {
    val filtered = when (filter) {
        PartyFilter.ALL -> this
        PartyFilter.WILL_GET -> filter { it.status == BalanceStatus.WILL_GET }
        PartyFilter.WILL_GIVE -> filter { it.status == BalanceStatus.WILL_GIVE }
        PartyFilter.SETTLED -> filter { it.status == BalanceStatus.SETTLED }
    }
    return when (sort) {
        PartySort.RECENT -> filtered
        PartySort.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.party.name })
        PartySort.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.balance.abs() }
    }
}
