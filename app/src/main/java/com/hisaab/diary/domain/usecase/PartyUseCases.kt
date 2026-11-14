package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.repository.PartyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Problems with party input. The UI turns these into messages. */
enum class PartyError { NAME_EMPTY, NAME_TOO_LONG, PHONE_INVALID, NOT_FOUND }

/** Checks party fields. Shared by add and update. */
object PartyValidator {
    const val MAX_NAME_LENGTH = 60
    private val phonePattern = Regex("""^\+?[0-9][0-9 \-]{5,19}$""")

    fun validate(name: String, phone: String?): PartyError? {
        val cleanName = name.trim()
        val cleanPhone = phone?.trim().orEmpty()
        return when {
            cleanName.isEmpty() -> PartyError.NAME_EMPTY
            cleanName.length > MAX_NAME_LENGTH -> PartyError.NAME_TOO_LONG
            cleanPhone.isNotEmpty() && !phonePattern.matches(cleanPhone) -> PartyError.PHONE_INVALID
            else -> null
        }
    }
}

class AddPartyUseCase @Inject constructor(private val repository: PartyRepository) {
    /** @return the new party id. */
    suspend operator fun invoke(bookId: Long, name: String, phone: String?, note: String?): Outcome<Long, PartyError> {
        PartyValidator.validate(name, phone)?.let { return Outcome.Failure(it) }
        return Outcome.Success(repository.addParty(bookId, name.trim(), phone?.trim(), note?.trim()))
    }
}

class UpdatePartyUseCase @Inject constructor(private val repository: PartyRepository) {
    suspend operator fun invoke(id: Long, name: String, phone: String?, note: String?): Outcome<Unit, PartyError> {
        PartyValidator.validate(name, phone)?.let { return Outcome.Failure(it) }
        val party = repository.getParty(id) ?: return Outcome.Failure(PartyError.NOT_FOUND)
        repository.updateParty(
            party.copy(
                name = name.trim(),
                phone = phone?.trim()?.ifEmpty { null },
                note = note?.trim()?.ifEmpty { null },
            ),
        )
        return Outcome.Success(Unit)
    }
}

class SetPartyArchivedUseCase @Inject constructor(private val repository: PartyRepository) {
    suspend operator fun invoke(id: Long, archived: Boolean) = repository.setArchived(id, archived)
}

/** Deletes a party and all its entries. The UI must confirm first. */
class DeletePartyUseCase @Inject constructor(private val repository: PartyRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteParty(id)
}

class ObservePartiesUseCase @Inject constructor(private val repository: PartyRepository) {
    operator fun invoke(bookId: Long, query: String = "", archived: Boolean = false): Flow<List<PartySummary>> =
        repository.observeParties(bookId, query, archived)
}

class ObservePartyUseCase @Inject constructor(private val repository: PartyRepository) {
    operator fun invoke(id: Long): Flow<PartySummary?> = repository.observeParty(id)
}

class GetPartyUseCase @Inject constructor(private val repository: PartyRepository) {
    suspend operator fun invoke(id: Long): Party? = repository.getParty(id)
}
