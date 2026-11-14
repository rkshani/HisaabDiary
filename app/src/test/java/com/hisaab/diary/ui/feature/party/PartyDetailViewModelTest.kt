package com.hisaab.diary.ui.feature.party

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.export.StatementExporter
import com.hisaab.diary.data.fake.FakeAttachmentStorage
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.usecase.AddTransactionUseCase
import com.hisaab.diary.domain.usecase.DeleteTransactionUseCase
import com.hisaab.diary.domain.usecase.ObserveLedgerUseCase
import com.hisaab.diary.domain.usecase.ObservePartyUseCase
import com.hisaab.diary.domain.usecase.RecentlyDeletedEntry
import com.hisaab.diary.domain.usecase.SettleUpUseCase
import com.hisaab.diary.domain.usecase.UndoDeleteTransactionUseCase
import com.hisaab.diary.util.MainDispatcherRule
import java.io.File
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PartyDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val dateUtils = DateUtils(clock = { 0 }, timeZone = utc)
    private val store = FakeStore()
    private val parties = FakePartyRepository(store)
    private val entries = FakeEntryRepository(store)

    private val recentlyDeleted = RecentlyDeletedEntry()
    private val attachments = FakeAttachmentStorage()
    private var exportResult: File? = File("statement.pdf")
    private var exportedRows = -1
    private val exporter = object : StatementExporter {
        override suspend fun export(summary: PartySummary, ledger: List<LedgerEntry>): File? {
            exportedRows = ledger.size
            return exportResult
        }
    }

    private fun viewModel(partyId: Long) = PartyDetailViewModel(
        SavedStateHandle(mapOf("partyId" to partyId)),
        ObservePartyUseCase(parties),
        ObserveLedgerUseCase(entries),
        dateUtils,
        recentlyDeleted,
        UndoDeleteTransactionUseCase(entries),
        attachments,
        SettleUpUseCase(parties, AddTransactionUseCase(entries) { 0 }, { 0 }),
        exporter,
    )

    @Test
    fun loadsPartyAndGroupsEntriesByDayNewestFirst() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        entries.addEntry(entry(ali, EntryType.GAVE, 1_000, date = DAY))
        entries.addEntry(entry(ali, EntryType.GOT, 300, date = DAY + HOUR))
        entries.addEntry(entry(ali, EntryType.GAVE, 50, date = 3 * DAY))

        viewModel(ali).uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.days.size < 2) state = awaitItem()
            assertEquals("Ali", state.summary!!.party.name)
            assertEquals(listOf(3 * DAY, DAY), state.days.map { it.dayStart })
            assertEquals(listOf(300L, 1_000L), state.days[1].entries.map { it.entry.amount.paisa })
            assertEquals(Money(-750), state.days[0].entries.single().runningBalance)
        }
    }

    @Test
    fun missingPartyGivesNullSummary() = runTest {
        viewModel(99).uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.summary)
        }
    }

    @Test
    fun undoRestoresDeletedEntryOfThisPartyOnly() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        val id = entries.addEntry(entry(ali, EntryType.GAVE, 1_000, date = DAY))
        val vm = viewModel(ali)

        val deleted = DeleteTransactionUseCase(entries)(id)!!
        recentlyDeleted.remember(deleted)
        assertEquals(deleted, vm.pendingUndo.value)

        vm.undo(deleted)
        assertNull(vm.pendingUndo.value)
        assertEquals(deleted, entries.getEntry(id))

        val other = entry(partyId = 999, type = EntryType.GOT, paisa = 5, date = 0)
        recentlyDeleted.remember(other)
        assertNull(vm.pendingUndo.value)
    }

    @Test
    fun dismissClearsPendingUndoAndDeletesPhoto() {
        val vm = viewModel(1)
        val e = entry(1, EntryType.GAVE, 5, 0).copy(attachmentPath = "/bills/a.jpg")
        recentlyDeleted.remember(e)
        vm.dismissUndo(e)
        assertNull(vm.pendingUndo.value)
        assertEquals(listOf("/bills/a.jpg"), attachments.deleted)
    }

    @Test
    fun settleUpMakesBalanceZero() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        entries.addEntry(entry(ali, EntryType.GAVE, 900, date = DAY))
        viewModel(ali).settleUp("Settled up")
        val last = store.entries.value.last()
        assertEquals(EntryType.GOT, last.type)
        assertEquals(Money(900), last.amount)
    }

    @Test
    fun exportPdfSendsShareEffectWithWholeLedger() = runTest {
        val ali = parties.addParty(1, "Ali", null, null)
        entries.addEntry(entry(ali, EntryType.GAVE, 100, date = DAY))
        entries.addEntry(entry(ali, EntryType.GOT, 50, date = 2 * DAY))
        val vm = viewModel(ali)
        vm.uiState.test {
            var state = awaitItem()
            while (state.summary == null) state = awaitItem()
            vm.effects.test {
                vm.exportPdf()
                assertEquals(PartyDetailEffect.SharePdf(File("statement.pdf")), awaitItem())
                exportResult = null
                vm.exportPdf()
                assertEquals(PartyDetailEffect.ExportFailed, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, exportedRows)
    }

    @Test
    fun groupByDayOfEmptyLedgerIsEmpty() {
        assertEquals(emptyList<LedgerDay>(), groupByDay(emptyList<LedgerEntry>(), dateUtils))
    }

    private fun entry(partyId: Long, type: EntryType, paisa: Long, date: Long) =
        Entry(0, partyId, type, Money(paisa), null, date, null, null, 0, 0)

    private companion object {
        const val HOUR = 60L * 60 * 1000
        const val DAY = 24 * HOUR
    }
}
