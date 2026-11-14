package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.fake.FakePartyRepository
import com.hisaab.diary.domain.fake.FakeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyUseCasesTest {
    private val store = FakeStore()
    private val repo = FakePartyRepository(store)
    private val add = AddPartyUseCase(repo)
    private val update = UpdatePartyUseCase(repo)
    private val observe = ObservePartiesUseCase(repo)

    @Test
    fun `add party trims and saves`() = runTest {
        val result = add(BOOK, "  Ali  ", " 0300-1234567 ", null)
        val id = (result as Outcome.Success).value
        val party = repo.getParty(id)!!
        assertEquals("Ali", party.name)
        assertEquals("0300-1234567", party.phone)
    }

    @Test
    fun `add party rejects bad input`() = runTest {
        assertEquals(Outcome.Failure(PartyError.NAME_EMPTY), add(BOOK, "   ", null, null))
        assertEquals(Outcome.Failure(PartyError.NAME_TOO_LONG), add(BOOK, "x".repeat(61), null, null))
        assertEquals(Outcome.Failure(PartyError.PHONE_INVALID), add(BOOK, "Ali", "abc", null))
        assertEquals(Outcome.Failure(PartyError.PHONE_INVALID), add(BOOK, "Ali", "12", null))
        assertTrue(store.parties.value.isEmpty())
    }

    @Test
    fun `valid phone formats`() {
        listOf("03001234567", "+92 300 1234567", "0300-1234567", "").forEach {
            assertNull("phone $it", PartyValidator.validate("Ali", it))
        }
    }

    @Test
    fun `update party changes fields and clears empty phone`() = runTest {
        val id = (add(BOOK, "Ali", "03001234567", null) as Outcome.Success).value
        assertEquals(Outcome.Success(Unit), update(id, "Ali Raza", "", "note"))
        val party = repo.getParty(id)!!
        assertEquals("Ali Raza", party.name)
        assertNull(party.phone)
        assertEquals("note", party.note)
    }

    @Test
    fun `update missing party fails`() = runTest {
        assertEquals(Outcome.Failure(PartyError.NOT_FOUND), update(99, "Ali", null, null))
    }

    @Test
    fun `archive delete and observe`() = runTest {
        val a = (add(BOOK, "A", null, null) as Outcome.Success).value
        add(BOOK, "B", null, null)
        SetPartyArchivedUseCase(repo)(a, true)
        assertEquals(listOf("B"), observe(BOOK).first().map { it.party.name })

        DeletePartyUseCase(repo)(a)
        assertNull(GetPartyUseCase(repo)(a))
        assertEquals("B", ObservePartyUseCase(repo)(store.parties.value.single().id).first()!!.party.name)
    }

    private companion object {
        const val BOOK = 1L
    }
}
