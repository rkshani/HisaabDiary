package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.repository.ReminderRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Called when an entry gets a due date: the party is reminded on that day. */
class SetPartyReminderUseCase @Inject constructor(private val reminders: ReminderRepository) {
    suspend operator fun invoke(partyId: Long, dueDate: Long) = reminders.setForParty(partyId, dueDate)
}

class ObserveRemindersUseCase @Inject constructor(private val reminders: ReminderRepository) {
    operator fun invoke(): Flow<List<Reminder>> = reminders.observeOpen()
}

class MarkReminderDoneUseCase @Inject constructor(private val reminders: ReminderRepository) {
    suspend operator fun invoke(id: Long) = reminders.markDone(id)
}

/** Moves a reminder to the same time [days] days from today. */
class SnoozeReminderUseCase @Inject constructor(
    private val reminders: ReminderRepository,
    private val dateUtils: DateUtils,
) {
    suspend operator fun invoke(id: Long, days: Int = 1) {
        val target = dateUtils.endOfDay(dateUtils.plusDays(dateUtils.now(), days))
        reminders.moveTo(id, target)
    }
}

/**
 * Reminders to show as notifications now: due by the end of today and the party still has
 * money open. Settled parties are closed first.
 */
class GetDueRemindersUseCase @Inject constructor(
    private val reminders: ReminderRepository,
    private val dateUtils: DateUtils,
) {
    suspend operator fun invoke(): List<Reminder> {
        reminders.closeSettled()
        val endOfToday = dateUtils.endOfDay(dateUtils.now())
        return reminders.observeOpen().first()
            .filter { it.remindAt <= endOfToday && it.status != BalanceStatus.SETTLED }
    }
}
