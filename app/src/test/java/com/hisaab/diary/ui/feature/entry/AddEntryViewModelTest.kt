package com.hisaab.diary.ui.feature.entry

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.AmountCalculator
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.fake.FakeAttachmentStorage
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeReminderRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.AddTransactionUseCase
import com.hisaab.diary.domain.usecase.DeleteTransactionUseCase
import com.hisaab.diary.domain.usecase.EntryError
import com.hisaab.diary.domain.usecase.GetPartyUseCase
import com.hisaab.diary.domain.usecase.GetTransactionUseCase
import com.hisaab.diary.domain.usecase.RecentlyDeletedEntry
import com.hisaab.diary.domain.usecase.SetPartyReminderUseCase
import com.hisaab.diary.domain.usecase.UpdateTransactionUseCase
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

    private val recentlyDeleted = RecentlyDeletedEntry()
    private val attachments = FakeAttachmentStorage()
    private val reminders = FakeReminderRepository()

    private fun viewModel(isGave: Boolean = true, entryId: Long? = null): AddEntryViewModel {
        val args = buildMap<String, Any> {
            put("partyId", partyId)
            put("isGave", isGave)
            entryId?.let { put("entryId", it) }
        }
        return AddEntryViewModel(
            SavedStateHandle(args),
            AddTransactionUseCase(entries) { NOW },
            UpdateTransactionUseCase(entries) { NOW },
            GetTransactionUseCase(entries),
            DeleteTransactionUseCase(entries),
            recentlyDeleted,
            GetPartyUseCase(parties),
            dateUtils,
            attachments,
            SetPartyReminderUseCase(reminders),
        )
    }

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
    fun editLoadsEntryAndUpdatesIt() = runTest {
        val id = entries.addEntry(Entry(0, partyId, EntryType.GOT, Money(125_050), "rent", DAY, null, null, 0, 0))
        val vm = viewModel(entryId = id)
        val loaded = vm.state.value
        assertTrue(loaded.isEdit)
        assertEquals("1250.50", loaded.expression)
        assertEquals("rent", loaded.note)
        assertEquals(EntryType.GOT, loaded.type)

        vm.effects.test {
            vm.onEvent(AddEntryEvent.ClearAmount)
            "300".forEach { vm.onEvent(AddEntryEvent.Key(it)) }
            vm.onEvent(AddEntryEvent.Save)
            assertEquals(AddEntryEffect.Saved, awaitItem())
        }
        assertEquals(Money.ofRupees(300), entries.getEntry(id)!!.amount)
        assertEquals(1, store.entries.value.size)
    }

    @Test
    fun deleteRemembersEntryForUndo() = runTest {
        val id = entries.addEntry(Entry(0, partyId, EntryType.GAVE, Money(500), null, DAY, null, null, 0, 0))
        val vm = viewModel(entryId = id)
        vm.effects.test {
            vm.onEvent(AddEntryEvent.Delete)
            assertEquals(AddEntryEffect.Deleted, awaitItem())
        }
        assertNull(entries.getEntry(id))
        assertEquals(id, recentlyDeleted.entry.value!!.id)
    }

    @Test
    fun photoIsAttachedReplacedAndSaved() = runTest {
        val vm = viewModel()
        vm.onEvent(AddEntryEvent.AttachmentPicked(Uri.parse("content://a")))
        assertEquals("/bills/photo1.jpg", vm.state.value.attachmentPath)

        // Replacing an unsaved photo deletes the first one.
        vm.onEvent(AddEntryEvent.AttachmentPicked(Uri.parse("content://b")))
        assertEquals("/bills/photo2.jpg", vm.state.value.attachmentPath)
        assertEquals(listOf("/bills/photo1.jpg"), attachments.deleted)

        "10".forEach { vm.onEvent(AddEntryEvent.Key(it)) }
        vm.effects.test {
            vm.onEvent(AddEntryEvent.Save)
            awaitItem()
        }
        assertEquals("/bills/photo2.jpg", store.entries.value.single().attachmentPath)
    }

    @Test
    fun editingReplacesOldPhotoOnlyAfterSave() = runTest {
        val id = entries.addEntry(
            Entry(0, partyId, EntryType.GAVE, Money(500), null, DAY, null, "/bills/old.jpg", 0, 0),
        )
        val vm = viewModel(entryId = id)
        vm.onEvent(AddEntryEvent.RemoveAttachment)
        assertNull(vm.state.value.attachmentPath)
        assertEquals(emptyList<String>(), attachments.deleted)

        vm.effects.test {
            vm.onEvent(AddEntryEvent.Save)
            awaitItem()
        }
        assertEquals(listOf("/bills/old.jpg"), attachments.deleted)
        assertNull(entries.getEntry(id)!!.attachmentPath)
    }

    @Test
    fun savingWithDueDateSetsPartyReminder() = runTest {
        val vm = viewModel()
        "5".forEach { vm.onEvent(AddEntryEvent.Key(it)) }
        vm.onEvent(AddEntryEvent.DueDatePicked(DAY * 25))
        vm.effects.test {
            vm.onEvent(AddEntryEvent.Save)
            awaitItem()
        }
        assertEquals(mapOf(partyId to DAY * 26 - 1), reminders.setFor)
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
