package com.hisaab.diary.ui.feature.reminders

import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeReminderRepository
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.usecase.MarkReminderDoneUseCase
import com.hisaab.diary.domain.usecase.ObserveRemindersUseCase
import com.hisaab.diary.domain.usecase.SnoozeReminderUseCase
import com.hisaab.diary.util.MainDispatcherRule
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RemindersViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val dateUtils = DateUtils(clock = { NOW }, timeZone = TimeZone.getTimeZone("UTC"))
    private val repo = FakeReminderRepository()

    private fun viewModel() = RemindersViewModel(
        ObserveRemindersUseCase(repo),
        MarkReminderDoneUseCase(repo),
        SnoozeReminderUseCase(repo, dateUtils),
        dateUtils,
    )

    @Test
    fun splitsIntoLateTodayAndUpcomingAndHidesSettled() = runTest {
        repo.open.value = listOf(
            reminder(1, DAY * 8, balance = -100),
            reminder(2, DAY * 10 + 1, balance = 200),
            reminder(3, DAY * 15, balance = -50),
            reminder(4, DAY * 9, balance = 0),
        )
        viewModel().uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(listOf(1L), state.overdue.map { it.id })
            assertEquals(listOf(2L), state.today.map { it.id })
            assertEquals(listOf(3L), state.upcoming.map { it.id })
        }
    }

    @Test
    fun doneAndSnooze() = runTest {
        repo.open.value = listOf(reminder(1, DAY * 8, balance = -100), reminder(2, DAY * 8, balance = -1))
        val vm = viewModel()
        vm.onDone(1)
        assertEquals(listOf(2L), repo.open.value.map { it.id })
        vm.onSnooze(2)
        assertEquals(DAY * 12 - 1, repo.open.value.single().remindAt)
    }

    @Test
    fun emptyState() = runTest {
        viewModel().uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertTrue(state.isEmpty)
        }
    }

    private fun reminder(id: Long, remindAt: Long, balance: Long) =
        Reminder(id, partyId = id, bookId = 1, partyName = "P$id", remindAt = remindAt, balance = Money(balance))

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        const val NOW = DAY * 10 + 9L * 60 * 60 * 1000
    }
}
