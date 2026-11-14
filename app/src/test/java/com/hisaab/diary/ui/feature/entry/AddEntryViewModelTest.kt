package com.hisaab.diary.ui.feature.entry

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.AmountCalculator
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.AddTransactionUseCase
import com.hisaab.diary.domain.usecase.EntryError
import com.hisaab.diary.domain.usecase.GetPartyUseCase
import com.hisaab.diary.util.MainDispatcherRule
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEntryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val dateUtils = DateUtils(clock = { NOW }, timeZone = utc)
    private val store = FakeStore()
    private val parties = FakePartyRepository(store)
    private val entries = FakeEntryRepository(store)
    private val partyId = runBlocking { parties.addParty(1, "Ali", null, null) }

    private fun viewModel(isGave: Boolean = true) = AddEntryViewModel(
        SavedStateHandle(mapOf("partyId" to partyId, "isGave" to isGave)),
        AddTransactionUseCase(entries) { NOW },
        GetPartyUseCase(parties),
        dateUtils,
    )

    @Test
    fun startsWithRouteTypeTodayAndPartyName() {
        val state = viewModel(isGave = false).state.value
        assertEquals(EntryType.GOT, state.type)
        assertEquals(NOW, state.date)
        assertEquals("Ali", state.partyName)
    }

    @Test
    fun keypadBuildsExpressionAndResult() {
        val vm = viewModel()
        "12".forEach { vm.onEvent(AddEntryEvent.Key(it)) }
        vm.onEvent(AddEntryEvent.Key(AmountCalculator.PLUS))
        vm.onEvent(AddEntryEvent.Key('8'))
        assertEquals("12${AmountCalculator.PLUS}8", vm.state.value.expression)
        assertEquals(Money.ofRupees(20), vm.state.value.amount)
        assertTrue(vm.state.value.showResult)

        vm.onEvent(AddEntryEvent.Backspace)
        assertNull(vm.state.value.amount)
        vm.onEvent(AddEntryEvent.ClearAmount)
        assertEquals("", vm.state.value.expression)
    }

    @Test
    fun saveWithoutAmountShowsError() {
        val vm = viewModel()
        vm.onEvent(AddEntryEvent.Save)
        assertEquals(EntryError.AMOUNT_NOT_POSITIVE, vm.state.value.error)
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun saveCreatesEntry() = runTest {
        val vm = viewModel()
        vm.effects.test {
            "250".forEach { vm.onEvent(AddEntryEvent.Key(it)) }
            vm.onEvent(AddEntryEvent.NoteChanged("chai"))
            vm.onEvent(AddEntryEvent.TypeChanged(EntryType.GOT))
            vm.onEvent(AddEntryEvent.Save)
            assertEquals(AddEntryEffect.Saved, awaitItem())
        }
        val saved = store.entries.value.single()
        assertEquals(Money.ofRupees(250), saved.amount)
        assertEquals(EntryType.GOT, saved.type)
        assertEquals("chai", saved.note)
        assertEquals(partyId, saved.partyId)
    }

    @Test
    fun datesFromPicker() {
        val vm = viewModel()
        vm.onEvent(AddEntryEvent.DatePicked(DAY * 10))
        assertEquals(DAY * 10 + (NOW - DAY * 20), vm.state.value.date)

        vm.onEvent(AddEntryEvent.DueDatePicked(DAY * 12))
        assertEquals(DAY * 13 - 1, vm.state.value.dueDate)

        vm.onEvent(AddEntryEvent.DueDatePicked(null))
        assertNull(vm.state.value.dueDate)
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000

        // Day 20 at 10:00 UTC.
        const val NOW = DAY * 20 + 10L * 60 * 60 * 1000
    }
}
