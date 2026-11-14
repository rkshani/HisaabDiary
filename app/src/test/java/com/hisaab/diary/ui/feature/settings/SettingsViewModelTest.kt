package com.hisaab.diary.ui.feature.settings

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.data.backup.BackupService
import com.hisaab.diary.domain.fake.FakeSettingsRepository
import com.hisaab.diary.domain.usecase.DisableAppLockUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.SetPinUseCase
import com.hisaab.diary.domain.usecase.VerifyPinUseCase
import com.hisaab.diary.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeSettingsRepository()
    private var backupPassword: String? = "unset"
    private val backup = object : BackupService {
        override suspend fun create(target: Uri, password: String?): Boolean {
            backupPassword = password
            return true
        }
    }
    private val vm = SettingsViewModel(
        ObserveSettingsUseCase(repo),
        SetPinUseCase(repo),
        DisableAppLockUseCase(repo),
        repo,
        backup,
    )

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
    fun backupUsesChosenPasswordAndReportsResult() = runTest {
        vm.messages.test {
            vm.startBackup("pw")
            vm.createBackup(Uri.EMPTY)
            assertEquals(SettingsMessage.BACKUP_DONE, awaitItem())
        }
        assertEquals("pw", backupPassword)
        vm.startBackup("")
        vm.createBackup(Uri.EMPTY)
        assertNull(backupPassword)
    }

    @Test
    fun badPinIsIgnored() = runTest {
        vm.enableLock("12")
        assertFalse(repo.settings.value.isLockEnabled)
    }
}
