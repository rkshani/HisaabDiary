package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.EntryRepository
import com.hisaab.diary.domain.repository.PartyRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Debug only: makes a "Test data" book with [parties] people and [entriesPerParty] entries each
 * (10,000 by default) to check that long lists stay smooth.
 */
class SeedTestDataUseCase @Inject constructor(
    private val books: BookRepository,
    private val parties: PartyRepository,
    private val entries: EntryRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(parties: Int = DEFAULT_PARTIES, entriesPerParty: Int = DEFAULT_ENTRIES): Int {
        val random = Random(SEED)
        val now = clock.nowMillis()
        val bookId = books.addBook(BOOK_NAME)
        repeat(parties) { p ->
            val partyId = this.parties.addParty(bookId, "Person ${p + 1}", null, null)
            val list = List(entriesPerParty) { i ->
                Entry(
                    id = 0,
                    partyId = partyId,
                    type = if (random.nextBoolean()) EntryType.GAVE else EntryType.GOT,
                    amount = Money(random.nextLong(MIN_PAISA, MAX_PAISA)),
                    note = "Test entry ${i + 1}",
                    date = now - (entriesPerParty - i) * HOUR,
                    dueDate = null,
                    attachmentPath = null,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            entries.addEntries(list)
        }
        settings.setSelectedBook(bookId)
        return parties * entriesPerParty
    }

    private companion object {
        const val BOOK_NAME = "Test data"
        const val DEFAULT_PARTIES = 100
        const val DEFAULT_ENTRIES = 100
        const val SEED = 42
        const val MIN_PAISA = 1_000L
        const val MAX_PAISA = 5_000_000L
        const val HOUR = 60L * 60 * 1000
    }
}
