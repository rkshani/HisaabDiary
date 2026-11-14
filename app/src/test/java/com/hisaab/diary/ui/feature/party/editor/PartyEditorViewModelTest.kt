package com.hisaab.diary.ui.feature.party.editor

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.data.contacts.ContactReader
import com.hisaab.diary.data.contacts.PickedContact
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.usecase.AddPartyUseCase
import com.hisaab.diary.domain.usecase.DeletePartyUseCase
import com.hisaab.diary.domain.usecase.GetPartyUseCase
import com.hisaab.diary.domain.usecase.PartyError
import com.hisaab.diary.domain.usecase.SetPartyArchivedUseCase
import com.hisaab.diary.domain.usecase.UpdatePartyUseCase
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PartyEditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = FakeStore()
    private val repo = FakePartyRepository(store)
    private var contact: PickedContact? = PickedContact("Ali Raza", "0300 1234567")

    private fun viewModel() = PartyEditorViewModel(
        AddPartyUseCase(repo),
        UpdatePartyUseCase(repo),
        GetPartyUseCase(repo),
        SetPartyArchivedUseCase(repo),
        DeletePartyUseCase(repo),
        object : ContactReader {
            override suspend fun read(uri: Uri): PickedContact? = contact
        },
    )

    @Test
    fun addNewParty() = runTest {
        val vm = viewModel().apply { start(BOOK, null) }
        vm.effects.test {
            vm.onEvent(PartyEditorEvent.NameChanged("Bilal"))
            vm.onEvent(PartyEditorEvent.Save)
            val saved = awaitItem() as PartyEditorEffect.Saved
            assertTrue(saved.isNew)
            assertEquals("Bilal", repo.getParty(saved.partyId)!!.name)
        }
    }

    @Test
    fun showsErrorForEmptyNameAndClearsItOnTyping() = runTest {
        val vm = viewModel().apply { start(BOOK, null) }
        vm.onEvent(PartyEditorEvent.Save)
        assertEquals(PartyError.NAME_EMPTY, vm.state.value.error)
        vm.onEvent(PartyEditorEvent.NameChanged("A"))
        assertNull(vm.state.value.error)
    }

    @Test
    fun contactFillsNameAndPhone() = runTest {
        val vm = viewModel().apply { start(BOOK, null) }
        vm.onEvent(PartyEditorEvent.ContactPicked(Uri.parse("content://contacts/1")))
        assertEquals("Ali Raza", vm.state.value.name)
        assertEquals("0300 1234567", vm.state.value.phone)
    }

    @Test
    fun cancelledOrEmptyContactKeepsFields() = runTest {
        contact = null
        val vm = viewModel().apply { start(BOOK, null) }
        vm.onEvent(PartyEditorEvent.NameChanged("Kept"))
        vm.onEvent(PartyEditorEvent.ContactPicked(Uri.parse("content://contacts/2")))
        assertEquals("Kept", vm.state.value.name)
    }

    @Test
    fun editLoadsAndUpdates() = runTest {
        val id = repo.addParty(BOOK, "Old", "03001234567", "n")
        val vm = viewModel().apply { start(BOOK, id) }
        assertEquals("Old", vm.state.value.name)
        assertTrue(vm.state.value.isEdit)

        vm.effects.test {
            vm.onEvent(PartyEditorEvent.NameChanged("New"))
            vm.onEvent(PartyEditorEvent.Save)
            assertEquals(PartyEditorEffect.Saved(id, isNew = false), awaitItem())
        }
        assertEquals("New", repo.getParty(id)!!.name)
    }

    @Test
    fun archiveAndDelete() = runTest {
        val id = repo.addParty(BOOK, "Ali", null, null)
        val vm = viewModel().apply { start(BOOK, id) }
        vm.effects.test {
            vm.onEvent(PartyEditorEvent.ToggleArchive)
            assertEquals(PartyEditorEffect.ArchivedChanged(true), awaitItem())
            assertTrue(repo.getParty(id)!!.isArchived)

            vm.onEvent(PartyEditorEvent.Delete)
            assertEquals(PartyEditorEffect.Deleted, awaitItem())
            assertNull(repo.getParty(id))
        }
    }

    private companion object {
        const val BOOK = 1L
    }
}
