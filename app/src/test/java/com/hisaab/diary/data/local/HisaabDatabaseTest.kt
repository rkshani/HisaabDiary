package com.hisaab.diary.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the schema rules (foreign keys, cascade delete, defaults) with plain SQL. */
@RunWith(AndroidJUnit4::class)
class HisaabDatabaseTest {
    private lateinit var db: HisaabDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HisaabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO books (id, name, created_at) VALUES (1, 'Personal', 0)")
        sql.execSQL("INSERT INTO parties (id, book_id, name, created_at) VALUES (10, 1, 'Ali', 0)")
        sql.execSQL(
            "INSERT INTO transactions (party_id, type, amount_paisa, date, created_at, updated_at) " +
                "VALUES (10, 'GAVE', 200000, 0, 0, 0)",
        )
        sql.execSQL("INSERT INTO reminders (party_id, remind_at) VALUES (10, 0)")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun defaultsAreFalse() {
        assertEquals(0L, count("SELECT is_archived FROM parties WHERE id = 10"))
        assertEquals(0L, count("SELECT is_done FROM reminders"))
    }

    @Test
    fun deletingPartyDeletesItsEntriesAndReminders() {
        db.openHelper.writableDatabase.execSQL("DELETE FROM parties WHERE id = 10")
        assertEquals(0L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(0L, count("SELECT COUNT(*) FROM reminders"))
    }

    @Test
    fun deletingBookDeletesEverything() {
        db.openHelper.writableDatabase.execSQL("DELETE FROM books WHERE id = 1")
        assertEquals(0L, count("SELECT COUNT(*) FROM parties"))
        assertEquals(0L, count("SELECT COUNT(*) FROM transactions"))
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun partyNeedsAnExistingBook() {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO parties (book_id, name, created_at) VALUES (99, 'Nobody', 0)",
        )
    }

    private fun count(query: String): Long = db.openHelper.readableDatabase.query(query).use {
        it.moveToFirst()
        it.getLong(0)
    }
}
