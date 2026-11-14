package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

enum class BookError { NAME_EMPTY, NAME_TOO_LONG, LAST_BOOK }

object BookValidator {
    const val MAX_NAME_LENGTH = 40

    fun validate(name: String): BookError? = when {
        name.isBlank() -> BookError.NAME_EMPTY
        name.trim().length > MAX_NAME_LENGTH -> BookError.NAME_TOO_LONG
        else -> null
    }
}

class ObserveBooksUseCase @Inject constructor(private val books: BookRepository) {
    operator fun invoke(): Flow<List<Book>> = books.observeBooks()
}

class SelectBookUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(bookId: Long) = settings.setSelectedBook(bookId)
}

/** Adds a book and switches to it. @return the new book id. */
class AddBookUseCase @Inject constructor(private val books: BookRepository, private val settings: SettingsRepository) {
    suspend operator fun invoke(name: String): Outcome<Long, BookError> {
        BookValidator.validate(name)?.let { return Outcome.Failure(it) }
        val id = books.addBook(name.trim())
        settings.setSelectedBook(id)
        return Outcome.Success(id)
    }
}

class RenameBookUseCase @Inject constructor(private val books: BookRepository) {
    suspend operator fun invoke(id: Long, name: String): Outcome<Unit, BookError> {
        BookValidator.validate(name)?.let { return Outcome.Failure(it) }
        books.renameBook(id, name.trim())
        return Outcome.Success(Unit)
    }
}

/**
 * Deletes a book with all its people and entries. The last book cannot be deleted.
 * If the deleted book was selected, the first other book is selected.
 */
class DeleteBookUseCase @Inject constructor(
    private val books: BookRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<Unit, BookError> {
        val all = books.observeBooks().first()
        if (all.size <= 1) return Outcome.Failure(BookError.LAST_BOOK)
        books.deleteBook(id)
        if (settings.settings.first().selectedBookId == id) {
            all.firstOrNull { it.id != id }?.let { settings.setSelectedBook(it.id) }
        }
        return Outcome.Success(Unit)
    }
}
