package com.hisaab.diary.domain.usecase

import com.hisaab.diary.core.security.PinHasher
import com.hisaab.diary.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Turns the app lock on with a new 4-digit PIN. @return false if the PIN is not 4 digits. */
class SetPinUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(pin: String): Boolean {
        if (!PinHasher.isValidPin(pin)) return false
        val salt = PinHasher.newSalt()
        settings.setPin(PinHasher.hash(pin, salt), salt)
        return true
    }
}

class DisableAppLockUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke() = settings.setPin(null, null)
}

class VerifyPinUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(pin: String): Boolean {
        val current = settings.settings.first()
        val hash = current.pinHash ?: return true
        val salt = current.pinSalt ?: return true
        return PinHasher.matches(pin, salt, hash)
    }
}
