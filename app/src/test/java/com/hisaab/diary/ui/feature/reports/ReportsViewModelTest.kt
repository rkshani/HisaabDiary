package com.hisaab.diary.ui.feature.reports

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.export.CsvExporter
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
        exporter,
    )

    private var exportedBook: Long? = -1
    private var exportResult: Int? = 3
    private val exporter = object : CsvExporter {
        override suspend fun export(bookId: Long?, target: Uri): Int? {
            exportedBook = bookId
            return exportResult
        }
    }

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

    @Test
    fun exportCsvUsesCurrentBookAndReportsResult() = runTest {
        val book = books.addBook("Shop")
        settings.setSelectedBook(book)
        val vm = viewModel()
        vm.uiState.test {
            awaitUntil { !it.isLoading }
            vm.effects.test {
                vm.exportCsv(Uri.EMPTY)
                assertEquals(ReportsEffect.CsvSaved(3), awaitItem())
                exportResult = null
                vm.exportCsv(Uri.EMPTY)
                assertEquals(ReportsEffect.CsvFailed, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(book, exportedBook)
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
