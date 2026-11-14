package com.hisaab.diary.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.ThemeMode
import com.hisaab.diary.ui.components.LocalMoneyFormatter
import com.hisaab.diary.ui.theme.HisaabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val moneyFormatter = remember(settings.currencySymbol) { MoneyFormatter(settings.currencySymbol) }
            HisaabTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalMoneyFormatter provides moneyFormatter) {
                    HisaabRoot()
                }
            }
        }
    }
}
