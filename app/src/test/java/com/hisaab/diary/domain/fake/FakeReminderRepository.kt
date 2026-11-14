package com.hisaab.diary.domain.fake

import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeReminderRepository : ReminderRepository {
    val open = MutableStateFlow<List<Reminder>>(emptyList())
    val setFor = mutableMapOf<Long, Long>()
    var closeSettledCalls = 0

    override fun observeOpen() = open

    override suspend fun setForParty(partyId: Long, remindAt: Long) {
        setFor[partyId] = remindAt
    }

    override suspend fun markDone(id: Long) = open.update { list -> list.filterNot { it.id == id } }

    override suspend fun moveTo(id: Long, remindAt: Long) =
        open.update { list -> list.map { if (it.id == id) it.copy(remindAt = remindAt) else it } }

    override suspend fun closeSettled() {
        closeSettledCalls++
    }
}
