package com.hisaab.diary.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.ThemeMode
import com.hisaab.diary.ui.components.LocalMoneyFormatter
import com.hisaab.diary.ui.feature.lock.LockScreen
import com.hisaab.diary.ui.theme.HisaabTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity because the fingerprint prompt (BiometricPrompt) needs it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val lockState by viewModel.lockState.collectAsStateWithLifecycle()
            val wrongPin by viewModel.wrongPin.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val moneyFormatter = remember(settings.currencySymbol, settings.hideAmounts) {
                MoneyFormatter(settings.currencySymbol, hidden = settings.hideAmounts)
            }
            val canUseBiometric = settings.biometricEnabled && biometricAvailable()
            LaunchedEffect(lockState, canUseBiometric) {
                if (lockState == LockState.LOCKED && canUseBiometric) showBiometricPrompt()
            }
            LaunchedEffect(settings.blockScreenshots) { applySecureFlag(settings.blockScreenshots) }
            HisaabTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalMoneyFormatter provides moneyFormatter) {
                    when (lockState) {
                        LockState.LOADING -> Surface(Modifier.fillMaxSize()) {}

                        LockState.LOCKED -> LockScreen(
                            wrongPin = wrongPin,
                            showBiometric = canUseBiometric,
                            onPinEnter = viewModel::unlockWithPin,
                            onPinEdit = viewModel::clearWrongPin,
                            onBiometricClick = ::showBiometricPrompt,
                        )

                        LockState.UNLOCKED -> HisaabRoot()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        // Rotation also stops the activity; that must not count as leaving the app.
        if (!isChangingConfigurations) viewModel.onAppBackground()
    }

    private fun biometricAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showBiometricPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometric()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_biometric_title))
            .setNegativeButtonText(getString(R.string.lock_use_pin))
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    /** FLAG_SECURE blocks screenshots and hides the app in the recent-apps screen. */
    private fun applySecureFlag(secure: Boolean) {
        if (secure) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
