package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EnsureDefaultBookUseCaseTest {
    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val settings = FakeSettingsRepository()
    private val ensure = EnsureDefaultBookUseCase(books, settings)

    @Test
    fun `first launch creates Personal and selects it`() = runTest {
        val id = ensure("Personal")
        assertEquals(listOf("Personal"), store.books.value.map { it.name })
        assertEquals(id, settings.settings.value.selectedBookId)
    }

    @Test
    fun `second launch changes nothing`() = runTest {
        val first = ensure("Personal")
        val second = ensure("Personal")
        assertEquals(first, second)
        assertEquals(1, store.books.value.size)
    }

    @Test
    fun `selects first book when the selected one was deleted`() = runTest {
        val shop = books.addBook("Shop")
        settings.setSelectedBook(999)
        assertEquals(shop, ensure("Personal"))
        assertEquals(1, store.books.value.size)
    }

    @Test
    fun `parallel calls create only one book`() = runTest {
        (1..5).map { async { ensure("Personal") } }.awaitAll()
        assertEquals(1, store.books.value.size)
    }
}
