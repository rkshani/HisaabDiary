package com.hisaab.diary.ui.feature.settings

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.hisaab.diary.data.backup.BackupService
import com.hisaab.diary.data.backup.RestoreResult
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
    private var encrypted: Boolean? = true
    private val backup = object : BackupService {
        override suspend fun create(target: Uri, password: String?): Boolean {
            backupPassword = password
            return true
        }

        override suspend fun needsPassword(source: Uri): Boolean? = encrypted

        override suspend fun restore(source: Uri, password: String?): RestoreResult =
            if (password == null || password == "right") RestoreResult.OK else RestoreResult.WRONG_PASSWORD
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
    fun restoreFlowWithPassword() {
        val file = Uri.parse("content://backup")
        vm.onRestoreFilePicked(file)
        assertEquals(RestoreStep.NeedPassword(file, wrong = false), vm.restore.value)
        vm.onRestorePassword(file, "bad")
        assertEquals(RestoreStep.Confirm(file, "bad"), vm.restore.value)
        vm.onRestoreConfirmed(file, "bad")
        assertEquals(RestoreStep.NeedPassword(file, wrong = true), vm.restore.value)
        vm.onRestoreConfirmed(file, "right")
        assertEquals(RestoreStep.Restart, vm.restore.value)
    }

    @Test
    fun plainBackupGoesStraightToConfirm() {
        encrypted = false
        val file = Uri.parse("content://plain")
        vm.onRestoreFilePicked(file)
        assertEquals(RestoreStep.Confirm(file, null), vm.restore.value)
        vm.onRestoreCancelled()
        assertEquals(RestoreStep.Idle, vm.restore.value)
    }

    @Test
    fun badPinIsIgnored() = runTest {
        vm.enableLock("12")
        assertFalse(repo.settings.value.isLockEnabled)
    }
}
