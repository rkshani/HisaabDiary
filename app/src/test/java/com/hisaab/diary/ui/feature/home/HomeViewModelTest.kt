package com.hisaab.diary.ui.feature.home

import app.cash.turbine.ReceiveTurbine
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
import com.hisaab.diary.domain.usecase.PartyFilter
import com.hisaab.diary.domain.usecase.PartySort
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val parties = FakePartyRepository(store)
    private val entries = FakeEntryRepository(store)
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
            val loaded = awaitUntil { !it.isLoading }
            assertEquals("Personal", loaded.bookName)
            assertEquals(BookTotals(Money.ZERO, Money.ZERO), loaded.totals)
            assertFalse(loaded.hasAnyParty)
        }
    }

    @Test
    fun `parties and totals update live and follow the selected book`() = runTest {
        val personal = books.addBook("Personal")
        val shop = books.addBook("Shop")
        settings.setSelectedBook(personal)
        val ali = parties.addParty(personal, "Ali", null, null)

        viewModel().uiState.test {
            assertEquals(listOf("Ali"), awaitUntil { it.parties.isNotEmpty() }.parties.map { it.party.name })

            entries.addEntry(entry(ali, EntryType.GAVE, 5_000))
            val updated = awaitUntil {
                it.totals?.willGet == Money(5_000) &&
                    it.parties.single().balance == Money(-5_000)
            }
            assertTrue(updated.hasAnyParty)

            settings.setSelectedBook(shop)
            val shopState = awaitUntil { it.bookName == "Shop" }
            assertTrue(shopState.parties.isEmpty())
        }
    }

    @Test
    fun `search filter and sort`() = runTest {
        val book = books.addBook("Personal")
        settings.setSelectedBook(book)
        val ali = parties.addParty(book, "Ali", null, null)
        val zara = parties.addParty(book, "Zara", null, null)
        parties.addParty(book, "Bilal", null, null)
        entries.addEntry(entry(ali, EntryType.GAVE, 100))
        entries.addEntry(entry(zara, EntryType.GAVE, 900))
        val vm = viewModel()

        vm.uiState.test {
            val all = awaitUntil { it.parties.size == 3 }
            // Tab counts: Ali and Zara owe you; nobody you owe; Bilal is settled (only on "All").
            assertEquals(2, all.willGetCount)
            assertEquals(0, all.willGiveCount)

            vm.onEvent(HomeEvent.QueryChanged("za"))
            assertEquals(listOf("Zara"), awaitUntil { it.query == "za" && it.parties.size == 1 }.names())

            vm.onEvent(HomeEvent.QueryChanged(""))
            vm.onEvent(HomeEvent.FilterChanged(PartyFilter.WILL_GET))
            vm.onEvent(HomeEvent.SortChanged(PartySort.HIGHEST_AMOUNT))
            val result = awaitUntil {
                it.filter == PartyFilter.WILL_GET && it.sort == PartySort.HIGHEST_AMOUNT &&
                    it.query.isEmpty()
            }
            assertEquals(listOf("Zara", "Ali"), result.names())

            vm.onEvent(HomeEvent.FilterChanged(PartyFilter.SETTLED))
            val settled = awaitUntil { it.filter == PartyFilter.SETTLED }
            assertEquals(listOf("Bilal"), settled.names())
            assertTrue(settled.hasAnyParty)
        }
    }

    @Test
    fun `no match keeps hasAnyParty true`() = runTest {
        val book = books.addBook("Personal")
        settings.setSelectedBook(book)
        parties.addParty(book, "Ali", null, null)
        val vm = viewModel()
        vm.uiState.test {
            awaitUntil { it.parties.isNotEmpty() }
            vm.onEvent(HomeEvent.QueryChanged("nobody"))
            val state = awaitUntil { it.query == "nobody" }
            assertTrue(state.parties.isEmpty())
            assertTrue(state.hasAnyParty)
        }
    }

    private suspend fun ReceiveTurbine<HomeUiState>.awaitUntil(check: (HomeUiState) -> Boolean): HomeUiState {
        var item = awaitItem()
        while (!check(item)) item = awaitItem()
        return item
    }

    private fun HomeUiState.names() = parties.map { it.party.name }

    private fun entry(partyId: Long, type: EntryType, paisa: Long) =
        Entry(0, partyId, type, Money(paisa), null, 0, null, null, 0, 0)
}
