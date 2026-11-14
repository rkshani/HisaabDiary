package com.hisaab.diary.domain.repository

import com.hisaab.diary.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    /** Open reminders, soonest first. Settled parties are closed automatically. */
    fun observeOpen(): Flow<List<Reminder>>

    /** Sets the reminder of a party to [remindAt] (one open reminder per party). */
    suspend fun setForParty(partyId: Long, remindAt: Long)

    suspend fun markDone(id: Long)

    suspend fun moveTo(id: Long, remindAt: Long)

    /** Marks reminders of settled parties as done. */
    suspend fun closeSettled()
}
