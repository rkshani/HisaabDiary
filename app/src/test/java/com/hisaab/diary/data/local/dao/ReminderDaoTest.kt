package com.hisaab.diary.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisaab.diary.data.local.HisaabDatabase
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType
import com.hisaab.diary.data.repository.ReminderRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var db: HisaabDatabase
    private lateinit var repo: ReminderRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            HisaabDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = ReminderRepositoryImpl(db.reminderDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun onePerPartyWithBalanceAndClosingWhenSettled() = runTest {
        val book = db.bookDao().insert(BookEntity(name = "Personal", createdAt = 0))
        val ali = db.partyDao().insert(PartyEntity(bookId = book, name = "Ali", createdAt = 0))
        db.transactionDao().insert(entry(ali, TransactionType.GAVE, 2_000))

        repo.setForParty(ali, remindAt = 100)
        repo.setForParty(ali, remindAt = 200)
        val open = repo.observeOpen().first().single()
        assertEquals(200L, open.remindAt)
        assertEquals("Ali", open.partyName)
        assertEquals(-2_000L, open.balance.paisa)

        repo.closeSettled()
        assertEquals(1, repo.observeOpen().first().size)

        db.transactionDao().insert(entry(ali, TransactionType.GOT, 2_000))
        repo.closeSettled()
        assertTrue(repo.observeOpen().first().isEmpty())
    }

    @Test
    fun markDoneAndMove() = runTest {
        val book = db.bookDao().insert(BookEntity(name = "Personal", createdAt = 0))
        val ali = db.partyDao().insert(PartyEntity(bookId = book, name = "Ali", createdAt = 0))
        repo.setForParty(ali, remindAt = 100)
        val id = repo.observeOpen().first().single().id
        repo.moveTo(id, 999)
        assertEquals(999L, repo.observeOpen().first().single().remindAt)
        repo.markDone(id)
        assertTrue(repo.observeOpen().first().isEmpty())
    }

    private fun entry(partyId: Long, type: TransactionType, paisa: Long) =
        TransactionEntity(partyId = partyId, type = type, amountPaisa = paisa, date = 0, createdAt = 0, updatedAt = 0)
}
