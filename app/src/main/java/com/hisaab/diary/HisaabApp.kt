package com.hisaab.diary

import android.app.Application
import com.hisaab.diary.core.coroutines.ApplicationScope
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

    override fun onCreate() {
        super.onCreate()
        // First launch: create the "Personal" book and select it.
        appScope.launch { ensureDefaultBook(getString(R.string.default_book_name)) }
    }
}
