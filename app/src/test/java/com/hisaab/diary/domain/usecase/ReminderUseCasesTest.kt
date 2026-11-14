package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeReminderRepository
import com.hisaab.diary.domain.model.Reminder
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderUseCasesTest {
    private val dateUtils = DateUtils(clock = { NOW }, timeZone = TimeZone.getTimeZone("UTC"))
    private val repo = FakeReminderRepository()

    @Test
    fun `due reminders are today or late and not settled`() = runTest {
        repo.open.value = listOf(
            reminder(1, remindAt = DAY * 10 + 5, balance = -100), // today, will get
            reminder(2, remindAt = DAY * 3, balance = 500), // late, will give
            reminder(3, remindAt = DAY * 11, balance = -100), // tomorrow
            reminder(4, remindAt = DAY * 9, balance = 0), // settled
        )
        val due = GetDueRemindersUseCase(repo, dateUtils)()
        assertEquals(listOf(1L, 2L), due.map { it.id })
        assertEquals(1, repo.closeSettledCalls)
    }

    @Test
    fun `snooze moves to end of the next day`() = runTest {
        repo.open.value = listOf(reminder(1, remindAt = DAY * 10, balance = -1))
        SnoozeReminderUseCase(repo, dateUtils)(1)
        assertEquals(DAY * 12 - 1, repo.open.value.single().remindAt)
    }

    @Test
    fun `mark done and set for party`() = runTest {
        repo.open.value = listOf(reminder(1, remindAt = 0, balance = -1))
        MarkReminderDoneUseCase(repo)(1)
        assertEquals(emptyList<Reminder>(), repo.open.value)

        SetPartyReminderUseCase(repo)(7, 123)
        assertEquals(mapOf(7L to 123L), repo.setFor)
    }

    private fun reminder(id: Long, remindAt: Long, balance: Long) =
        Reminder(id, partyId = id, bookId = 1, partyName = "P$id", remindAt = remindAt, balance = Money(balance))

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        const val NOW = DAY * 10 + 9L * 60 * 60 * 1000
    }
}
