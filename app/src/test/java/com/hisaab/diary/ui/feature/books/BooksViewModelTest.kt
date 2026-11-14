package com.hisaab.diary.ui.feature.books

import app.cash.turbine.test
import com.hisaab.diary.domain.fake.FakeBookRepository
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.fake.FakeStore
import com.hisaab.diary.domain.usecase.AddBookUseCase
import com.hisaab.diary.domain.usecase.BookError
import com.hisaab.diary.domain.usecase.DeleteBookUseCase
import com.hisaab.diary.domain.usecase.ObserveBooksUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.RenameBookUseCase
import com.hisaab.diary.domain.usecase.SelectBookUseCase
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BooksViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = FakeStore()
    private val books = FakeBookRepository(store)
    private val settings = FakeSettingsRepository()

    private fun viewModel() = BooksViewModel(
        ObserveBooksUseCase(books),
        ObserveSettingsUseCase(settings),
        SelectBookUseCase(settings),
        AddBookUseCase(books, settings),
        RenameBookUseCase(books),
        DeleteBookUseCase(books, settings),
    )

    @Test
    fun addSelectsAndCallsDone() = runTest {
        val vm = viewModel()
        var done = false
        vm.uiState.test {
            awaitItem()
            vm.onAdd("Shop") { done = true }
            var state = awaitItem()
            while (state.books.isEmpty() || state.selectedId == null) state = awaitItem()
            assertEquals(listOf("Shop"), state.books.map { it.name })
            assertEquals(state.books.single().id, state.selectedId)
        }
        assertTrue(done)
    }

    @Test
    fun errorsAreShownAndCleared() = runTest {
        books.addBook("Personal")
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onAdd("") {}
            var state = awaitItem()
            while (state.error == null) state = awaitItem()
            assertEquals(BookError.NAME_EMPTY, state.error)

            vm.clearError()
            state = awaitItem()
            while (state.error != null) state = awaitItem()
            assertNull(state.error)

            vm.onDelete(store.books.value.single().id) {}
            state = awaitItem()
            while (state.error == null) state = awaitItem()
            assertEquals(BookError.LAST_BOOK, state.error)
        }
    }

    @Test
    fun selectChangesSettings() = runTest {
        viewModel().onSelect(5)
        assertEquals(5L, settings.settings.value.selectedBookId)
    }
}
