package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.fake.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckBackupReminderUseCaseTest {
    private val repo = FakeSettingsRepository()
    private var now = 0L
    private val check = CheckBackupReminderUseCase(repo) { now }

    @Test
    fun `first check starts the clock`() = runTest {
        now = 5 * DAY
        assertNull(check())
        assertEquals(5 * DAY, repo.settings.value.lastBackupAt)
    }

    @Test
    fun `reminds after the chosen days`() = runTest {
        repo.setLastBackupAt(0)
        now = 6 * DAY
        assertNull(check())
        now = 7 * DAY
        assertEquals(7, check())
    }

    @Test
    fun `zero days means never`() = runTest {
        repo.setLastBackupAt(0)
        repo.setBackupReminderDays(0)
        now = 100 * DAY
        assertNull(check())
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
