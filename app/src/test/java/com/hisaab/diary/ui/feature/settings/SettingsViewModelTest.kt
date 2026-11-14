package com.hisaab.diary.ui.feature.settings

import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.usecase.DisableAppLockUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.SetPinUseCase
import com.hisaab.diary.domain.usecase.VerifyPinUseCase
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeSettingsRepository()
    private val vm =
        SettingsViewModel(ObserveSettingsUseCase(repo), SetPinUseCase(repo), DisableAppLockUseCase(repo), repo)

    @Test
    fun enableVerifyAndDisableLock() = runTest {
        vm.enableLock("2580")
        assertTrue(repo.settings.value.isLockEnabled)
        assertTrue(VerifyPinUseCase(repo)("2580"))
        assertFalse(VerifyPinUseCase(repo)("0000"))

        vm.setBiometric(true)
        vm.disableLock()
        assertFalse(repo.settings.value.isLockEnabled)
        assertFalse(repo.settings.value.biometricEnabled)
    }

    @Test
    fun timeoutIsSaved() {
        vm.setLockTimeout(60)
        assertEquals(60, repo.settings.value.lockTimeoutSeconds)
    }

    @Test
    fun privacyToggles() {
        vm.setHideAmounts(true)
        vm.setBlockScreenshots(true)
        assertTrue(repo.settings.value.hideAmounts)
        assertTrue(repo.settings.value.blockScreenshots)
    }

    @Test
    fun badPinIsIgnored() = runTest {
        vm.enableLock("12")
        assertFalse(repo.settings.value.isLockEnabled)
    }
}
