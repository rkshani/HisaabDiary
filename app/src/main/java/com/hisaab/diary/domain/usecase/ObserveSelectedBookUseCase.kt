package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** The book chosen in settings. Emits again when the user switches book or renames it. */
class ObserveSelectedBookUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val books: BookRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Book> = settings.settings
        .map { it.selectedBookId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id -> books.observeBooks().map { list -> list.find { it.id == id } } }
        .filterNotNull()
        .distinctUntilChanged()
}
