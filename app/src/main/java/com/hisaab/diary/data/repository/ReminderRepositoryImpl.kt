package com.hisaab.diary.data.repository

import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.local.dao.ReminderDao
import com.hisaab.diary.data.local.entity.ReminderEntity
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.repository.ReminderRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ReminderRepository {
    override fun observeOpen(): Flow<List<Reminder>> = dao.observeOpen().map { rows ->
        rows.map {
            Reminder(
                id = it.id,
                partyId = it.partyId,
                bookId = it.bookId,
                partyName = it.partyName,
                remindAt = it.remindAt,
                balance = Money(it.balance),
            )
        }
    }

    override suspend fun setForParty(partyId: Long, remindAt: Long) = withContext(io) {
        val open = dao.getOpenForParty(partyId)
        if (open == null) {
            dao.insert(ReminderEntity(partyId = partyId, remindAt = remindAt))
        } else {
            dao.setRemindAt(open.id, remindAt)
        }
        Unit
    }

    override suspend fun markDone(id: Long) = withContext(io) { dao.markDone(id) }

    override suspend fun moveTo(id: Long, remindAt: Long) = withContext(io) { dao.setRemindAt(id, remindAt) }

    override suspend fun closeSettled() = withContext(io) { dao.closeSettled() }
}
