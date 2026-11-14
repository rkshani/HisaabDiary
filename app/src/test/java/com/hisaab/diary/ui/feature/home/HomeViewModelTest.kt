package com.hisaab.diary.ui.feature.home

import app.cash.turbine.test
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.ObserveBookTotalsUseCase
import com.hisaab.diary.domain.usecase.ObserveSelectedBookUseCase
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val settings = FakeSettingsRepository()

    private fun viewModel() = HomeViewModel(
        ObserveSelectedBookUseCase(settings, books),
        ObserveBookTotalsUseCase(books),
    )

    @Test
    fun `loading until a book is selected`() = runTest {
        viewModel().uiState.test {
            assertTrue(awaitItem().isLoading)
            val id = books.addBook("Personal")
            settings.setSelectedBook(id)
            val loaded = awaitItem()
            assertEquals("Personal", loaded.bookName)
            assertEquals(BookTotals(Money.ZERO, Money.ZERO), loaded.totals)
        }
    }

    @Test
    fun `totals update when entries change and follow the selected book`() = runTest {
        val personal = books.addBook("Personal")
        val shop = books.addBook("Shop")
        settings.setSelectedBook(personal)
        val ali = FakePartyRepository(store).addParty(personal, "Ali", null, null)

        viewModel().uiState.test {
            assertEquals(BookTotals(Money.ZERO, Money.ZERO), expectMostRecentItem().totals)

            FakeEntryRepository(store).addEntry(entry(ali, EntryType.GAVE, 5_000))
            assertEquals(Money(5_000), awaitItem().totals!!.willGet)

            settings.setSelectedBook(shop)
            val shopState = awaitItem()
            assertEquals("Shop", shopState.bookName)
            assertEquals(Money.ZERO, shopState.totals!!.willGet)
        }
    }

    private fun entry(partyId: Long, type: EntryType, paisa: Long) =
        Entry(0, partyId, type, Money(paisa), null, 0, null, null, 0, 0)
}
