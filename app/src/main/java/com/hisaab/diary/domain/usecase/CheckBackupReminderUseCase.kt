package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Daily check: is a "please back up" reminder due?
 * The first check only starts the clock, so new users are not reminded on day one.
 * @return days since the last backup when a reminder should show, else null.
 */
class CheckBackupReminderUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Int? {
        val current = settings.settings.first()
        val now = clock.nowMillis()
        val last = current.lastBackupAt
        if (last == null) {
            settings.setLastBackupAt(now)
            return null
        }
        val days = ((now - last) / MILLIS_PER_DAY).toInt()
        return days.takeIf { current.backupReminderDays > 0 && it >= current.backupReminderDays }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
