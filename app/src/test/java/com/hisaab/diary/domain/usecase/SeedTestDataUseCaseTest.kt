package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeEntryRepository
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.model.runningBalance
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedTestDataUseCaseTest {
    private val store = FakeStore()
    private val settings = FakeSettingsRepository()

    @Test
    fun `seeds people and entries and selects the book`() = runTest {
        val seed = SeedTestDataUseCase(
            FakeBookRepository(store),
            FakePartyRepository(store),
            FakeEntryRepository(store),
            settings,
        ) { 1_000_000_000L }
        assertEquals(30, seed(parties = 3, entriesPerParty = 10))
        assertEquals(3, store.parties.value.size)
        assertEquals(30, store.entries.value.size)
        assertEquals(store.books.value.single().id, settings.settings.value.selectedBookId)
    }

    @Test
    fun `running balance of 10000 entries is fast`() {
        val list = List(10_000) { i ->
            com.hisaab.diary.domain.model.Entry(
                i.toLong(), 1, com.hisaab.diary.domain.model.EntryType.GAVE,
                com.hisaab.diary.core.money.Money(100), null, i.toLong(), null, null, 0, 0,
            )
        }
        val millis = measureTimeMillis { runningBalance(list) }
        assertTrue("took $millis ms", millis < 500)
    }
}
