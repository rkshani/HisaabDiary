package com.hisaab.diary.ui.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.usecase.AddBookUseCase
import com.hisaab.diary.domain.usecase.BookError
import com.hisaab.diary.domain.usecase.DeleteBookUseCase
import com.hisaab.diary.domain.usecase.ObserveBooksUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.RenameBookUseCase
import com.hisaab.diary.domain.usecase.SelectBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BooksUiState(
    val books: List<Book> = emptyList(),
    val selectedId: Long? = null,
    val error: BookError? = null,
)

@HiltViewModel
class BooksViewModel @Inject constructor(
    observeBooks: ObserveBooksUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val selectBook: SelectBookUseCase,
    private val addBook: AddBookUseCase,
    private val renameBook: RenameBookUseCase,
    private val deleteBook: DeleteBookUseCase,
) : ViewModel() {
    private val error = MutableStateFlow<BookError?>(null)

    val uiState: StateFlow<BooksUiState> =
        combine(observeBooks(), observeSettings(), error) { books, settings, err ->
            BooksUiState(books = books, selectedId = settings.selectedBookId, error = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BooksUiState())

    fun onSelect(id: Long) {
        viewModelScope.launch { selectBook(id) }
    }

    /** @param onDone called when the action worked, so the dialog can close. */
    fun onAdd(name: String, onDone: () -> Unit) = launchResult({ addBook(name) }, onDone)

    fun onRename(id: Long, name: String, onDone: () -> Unit) = launchResult({ renameBook(id, name) }, onDone)

    fun onDelete(id: Long, onDone: () -> Unit) = launchResult({ deleteBook(id) }, onDone)

    fun clearError() {
        error.value = null
    }

    private fun launchResult(action: suspend () -> Outcome<*, BookError>, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = action()) {
                is Outcome.Success -> {
                    error.value = null
                    onDone()
                }

                is Outcome.Failure -> error.value = result.error
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
