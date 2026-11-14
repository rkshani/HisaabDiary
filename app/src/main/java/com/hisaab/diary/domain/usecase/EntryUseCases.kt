package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.core.money.MoneyParser
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.EntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Problems with entry input. */
enum class EntryError { AMOUNT_NOT_POSITIVE, AMOUNT_TOO_BIG, DUE_BEFORE_DATE, NOTE_TOO_LONG, NOT_FOUND }

/** The fields a user can type for an entry. */
data class EntryInput(
    val partyId: Long,
    val type: EntryType,
    val amount: Money,
    val note: String?,
    val date: Long,
    val dueDate: Long?,
    val attachmentPath: String? = null,
)

object EntryValidator {
    const val MAX_NOTE_LENGTH = 200

    fun validate(input: EntryInput): EntryError? = when {
        !input.amount.isPositive -> EntryError.AMOUNT_NOT_POSITIVE
        input.amount > MoneyParser.MAX -> EntryError.AMOUNT_TOO_BIG
        input.dueDate != null && input.dueDate < input.date -> EntryError.DUE_BEFORE_DATE
        (input.note?.trim()?.length ?: 0) > MAX_NOTE_LENGTH -> EntryError.NOTE_TOO_LONG
        else -> null
    }
}

class AddTransactionUseCase @Inject constructor(private val repository: EntryRepository, private val clock: Clock) {
    /** @return the new entry id. */
    suspend operator fun invoke(input: EntryInput): Outcome<Long, EntryError> {
        EntryValidator.validate(input)?.let { return Outcome.Failure(it) }
        val now = clock.nowMillis()
        val entry = Entry(
            id = 0,
            partyId = input.partyId,
            type = input.type,
            amount = input.amount,
            note = input.note?.trim()?.ifEmpty { null },
            date = input.date,
            dueDate = input.dueDate,
            attachmentPath = input.attachmentPath,
            createdAt = now,
            updatedAt = now,
        )
        return Outcome.Success(repository.addEntry(entry))
    }
}

class UpdateTransactionUseCase @Inject constructor(private val repository: EntryRepository, private val clock: Clock) {
    suspend operator fun invoke(id: Long, input: EntryInput): Outcome<Unit, EntryError> {
        EntryValidator.validate(input)?.let { return Outcome.Failure(it) }
        val old = repository.getEntry(id) ?: return Outcome.Failure(EntryError.NOT_FOUND)
        repository.updateEntry(
            old.copy(
                type = input.type,
                amount = input.amount,
                note = input.note?.trim()?.ifEmpty { null },
                date = input.date,
                dueDate = input.dueDate,
                attachmentPath = input.attachmentPath,
                updatedAt = clock.nowMillis(),
            ),
        )
        return Outcome.Success(Unit)
    }
}

/**
 * Deletes an entry and returns it, so the UI can offer Undo
 * with [UndoDeleteTransactionUseCase]. Returns null if it did not exist.
 */
class DeleteTransactionUseCase @Inject constructor(private val repository: EntryRepository) {
    suspend operator fun invoke(id: Long): Entry? {
        val entry = repository.getEntry(id) ?: return null
        repository.deleteEntry(id)
        return entry
    }
}

class UndoDeleteTransactionUseCase @Inject constructor(private val repository: EntryRepository) {
    suspend operator fun invoke(deleted: Entry) = repository.restoreEntry(deleted)
}

class GetTransactionUseCase @Inject constructor(private val repository: EntryRepository) {
    suspend operator fun invoke(id: Long): Entry? = repository.getEntry(id)
}

class ObserveLedgerUseCase @Inject constructor(private val repository: EntryRepository) {
    /** Entries oldest first with running balance. */
    operator fun invoke(partyId: Long): Flow<List<LedgerEntry>> = repository.observeLedger(partyId)
}

class ObserveBookTotalsUseCase @Inject constructor(private val repository: BookRepository) {
    operator fun invoke(bookId: Long): Flow<BookTotals> = repository.observeTotals(bookId)
}
