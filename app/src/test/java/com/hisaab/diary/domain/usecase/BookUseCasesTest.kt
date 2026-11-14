package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BookUseCasesTest {
    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val settings = FakeSettingsRepository()

    @Test
    fun `add selects the new book and validates`() = runTest {
        val add = AddBookUseCase(books, settings)
        val id = (add("  Shop ") as Outcome.Success).value
        assertEquals("Shop", books.getBook(id)!!.name)
        assertEquals(id, settings.settings.value.selectedBookId)
        assertEquals(Outcome.Failure(BookError.NAME_EMPTY), add("  "))
        assertEquals(Outcome.Failure(BookError.NAME_TOO_LONG), add("x".repeat(41)))
    }

    @Test
    fun `rename validates`() = runTest {
        val id = books.addBook("Old")
        assertEquals(Outcome.Success(Unit), RenameBookUseCase(books)(id, "Ghar"))
        assertEquals("Ghar", books.getBook(id)!!.name)
        assertEquals(Outcome.Failure(BookError.NAME_EMPTY), RenameBookUseCase(books)(id, ""))
    }

    @Test
    fun `cannot delete the last book`() = runTest {
        val only = books.addBook("Personal")
        assertEquals(Outcome.Failure(BookError.LAST_BOOK), DeleteBookUseCase(books, settings)(only))
        assertEquals(1, store.books.value.size)
    }

    @Test
    fun `deleting the selected book selects another`() = runTest {
        val personal = books.addBook("Personal")
        val shop = books.addBook("Shop")
        settings.setSelectedBook(shop)
        assertEquals(Outcome.Success(Unit), DeleteBookUseCase(books, settings)(shop))
        assertEquals(personal, settings.settings.value.selectedBookId)
        assertEquals(listOf("Personal"), store.books.value.map { it.name })
    }

    @Test
    fun `select book`() = runTest {
        SelectBookUseCase(settings)(9)
        assertEquals(9L, settings.settings.value.selectedBookId)
    }
}
