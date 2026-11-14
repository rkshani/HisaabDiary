package com.hisaab.diary.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hisaab.diary.core.money.CurrencySymbol
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val repo by lazy {
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                tmp.newFile("settings.preferences_pb").also { it.delete() }
            },
        )
    }

    @Test
    fun defaultsWhenEmpty() = scope.runTest {
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test
    fun savesAndReadsAllValues() = scope.runTest {
        repo.setSelectedBook(7)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setDynamicColor(false)
        repo.setCurrencySymbol(CurrencySymbol.RUPEE_SIGN)
        repo.setOnboardingDone(true)

        assertEquals(
            AppSettings(
                selectedBookId = 7,
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                currencySymbol = CurrencySymbol.RUPEE_SIGN,
                onboardingDone = true,
            ),
            repo.settings.first(),
        )
    }
}
