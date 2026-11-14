package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Makes sure there is at least one book and that a valid book is selected.
 * Runs on every app start; does nothing when all is fine.
 * @param defaultBookName name for the first book (comes from strings.xml, e.g. "Personal").
 * @return the selected book id.
 */
class EnsureDefaultBookUseCase @Inject constructor(
    private val books: BookRepository,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(defaultBookName: String): Long = mutex.withLock {
        val allBooks = books.observeBooks().first()
        val selected = settings.settings.first().selectedBookId
        when {
            allBooks.isEmpty() -> books.addBook(defaultBookName).also { settings.setSelectedBook(it) }
            allBooks.none { it.id == selected } -> allBooks.first().id.also { settings.setSelectedBook(it) }
            else -> requireNotNull(selected)
        }
    }
}

class CompleteOnboardingUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke() = settings.setOnboardingDone(true)
}

class ObserveSettingsUseCase @Inject constructor(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = settings.settings
}
