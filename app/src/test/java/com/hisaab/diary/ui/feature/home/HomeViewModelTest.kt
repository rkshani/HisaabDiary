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
import com.hisaab.diary.domain.usecase.ObservePartiesUseCase
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
    private val parties = FakePartyRepository(store)
    private val settings = FakeSettingsRepository()

    private fun viewModel() = HomeViewModel(
        ObserveSelectedBookUseCase(settings, books),
        ObserveBookTotalsUseCase(books),
        ObservePartiesUseCase(parties),
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
            assertTrue(loaded.parties.isEmpty())
        }
    }

    @Test
    fun `parties and totals update live and follow the selected book`() = runTest {
        val personal = books.addBook("Personal")
        val shop = books.addBook("Shop")
        settings.setSelectedBook(personal)
        val ali = parties.addParty(personal, "Ali", null, null)

        viewModel().uiState.test {
            val first = expectMostRecentItem()
            assertEquals(listOf("Ali"), first.parties.map { it.party.name })

            FakeEntryRepository(store).addEntry(Entry(0, ali, EntryType.GAVE, Money(5_000), null, 0, null, null, 0, 0))
            // Totals and parties are separate flows, so wait until both have updated.
            var updated = awaitItem()
            while (updated.parties.single().balance != Money(-5_000) || updated.totals!!.willGet != Money(5_000)) {
                updated = awaitItem()
            }
            assertEquals(Money(5_000), updated.totals!!.willGet)
            assertEquals(Money(-5_000), updated.parties.single().balance)

            settings.setSelectedBook(shop)
            var shopState = awaitItem()
            while (shopState.bookName != "Shop") shopState = awaitItem()
            assertEquals("Shop", shopState.bookName)
            assertTrue(shopState.parties.isEmpty())
        }
    }
}
