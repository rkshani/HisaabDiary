package com.hisaab.diary.ui.feature.reports

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.ObserveEntriesBetweenUseCase
import com.hisaab.diary.domain.usecase.ObservePartiesUseCase
import com.hisaab.diary.domain.usecase.ObserveSelectedBookUseCase
import com.hisaab.diary.util.MainDispatcherRule
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReportsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val dateUtils = DateUtils(clock = { NOW }, timeZone = TimeZone.getTimeZone("UTC"))
    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val parties = FakePartyRepository(store)
    private val entries = FakeEntryRepository(store)
    private val settings = FakeSettingsRepository()

    private fun viewModel() = ReportsViewModel(
        ObserveSelectedBookUseCase(settings, books),
        ObservePartiesUseCase(parties),
        ObserveEntriesBetweenUseCase(entries),
        dateUtils,
    )

    @Test
    fun todayRangeShowsTodaysEntriesWithPartyNames() = runTest {
        val book = books.addBook("Personal")
        settings.setSelectedBook(book)
        val ali = parties.addParty(book, "Ali", null, null)
        entries.addEntry(Entry(0, ali, EntryType.GAVE, Money(500), "tea", NOW - 1000, null, null, 0, 0))
        entries.addEntry(Entry(0, ali, EntryType.GOT, Money(200), null, NOW - 3 * DAY, null, null, 0, 0))
        val vm = viewModel()

        vm.uiState.test {
            vm.onRangeSelected(ReportRange.TODAY)
            val state = awaitUntil { it.range == ReportRange.TODAY && !it.isLoading }
            assertEquals(listOf("Ali"), state.entries.map { it.partyName })
            assertEquals(Money(500), state.report!!.totalGave)
            assertEquals(Money.ZERO, state.report!!.totalGot)
        }
    }

    @Test
    fun customRangeUsesPickedDates() = runTest {
        val book = books.addBook("Personal")
        settings.setSelectedBook(book)
        val vm = viewModel()
        vm.uiState.test {
            vm.onCustomRange(DAY * 2, DAY * 4)
            val state = awaitUntil { it.range == ReportRange.CUSTOM && !it.isLoading }
            assertEquals(DAY * 2, state.from)
            assertEquals(DAY * 5 - 1, state.to)
            assertEquals(3, state.report!!.buckets.size)
        }
    }

    private suspend fun ReceiveTurbine<ReportsUiState>.awaitUntil(check: (ReportsUiState) -> Boolean): ReportsUiState {
        var item = awaitItem()
        while (!check(item)) item = awaitItem()
        return item
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        const val NOW = DAY * 40 + 10L * 60 * 60 * 1000
    }
}
