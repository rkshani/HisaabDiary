package com.hisaab.diary.ui

import com.hisaab.diary.domain.fake.FakeSettingsRepository
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

class MainViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeSettingsRepository()
    private var now = 0L

    private fun viewModel() = MainViewModel(ObserveSettingsUseCase(repo), VerifyPinUseCase(repo)) { now }

    @Test
    fun noLockMeansUnlocked() {
        assertEquals(LockState.UNLOCKED, viewModel().lockState.value)
    }

    @Test
    fun lockedOnStartThenPinUnlocks() = runTest {
        SetPinUseCase(repo)("1234")
        val vm = viewModel()
        assertEquals(LockState.LOCKED, vm.lockState.value)

        vm.unlockWithPin("9999")
        assertTrue(vm.wrongPin.value)
        assertEquals(LockState.LOCKED, vm.lockState.value)

        vm.unlockWithPin("1234")
        assertFalse(vm.wrongPin.value)
        assertEquals(LockState.UNLOCKED, vm.lockState.value)
    }

    @Test
    fun locksAgainOnlyAfterTimeout() = runTest {
        SetPinUseCase(repo)("1234")
        repo.setLockTimeoutSeconds(30)
        val vm = viewModel()
        vm.unlockWithBiometric()

        vm.onAppBackground()
        now = 10_000
        vm.onAppForeground()
        assertEquals(LockState.UNLOCKED, vm.lockState.value)

        vm.onAppBackground()
        now = 50_000
        vm.onAppForeground()
        assertEquals(LockState.LOCKED, vm.lockState.value)
    }
}
