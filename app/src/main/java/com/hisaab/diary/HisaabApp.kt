package com.hisaab.diary

import android.app.Application
import com.hisaab.diary.core.coroutines.ApplicationScope
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.data.reminders.DueReminderWorker
import com.hisaab.diary.data.reminders.ReminderNotifications
import com.hisaab.diary.domain.usecase.EnsureDefaultBookUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class HisaabApp : Application() {
    @Inject
    lateinit var ensureDefaultBook: EnsureDefaultBookUseCase

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    @Inject
    lateinit var dateUtils: DateUtils

    override fun onCreate() {
        super.onCreate()
        // First launch: create the "Personal" book and select it.
        appScope.launch { ensureDefaultBook(getString(R.string.default_book_name)) }
        ReminderNotifications.createChannel(this)
        DueReminderWorker.schedule(this, dateUtils)
    }
}
