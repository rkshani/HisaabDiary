package com.hisaab.diary.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.balanceStatus
import com.hisaab.diary.ui.theme.HisaabTheme

/** Formats money with the currency symbol from settings. Provided in MainActivity. */
val LocalMoneyFormatter = staticCompositionLocalOf { MoneyFormatter() }

/** `Rs 1,250` using the current formatter. Shows the absolute value; color tells the side. */
@Composable
@ReadOnlyComposable
fun formatMoney(money: Money): String = LocalMoneyFormatter.current.format(money)

/** Green when you will get, red when you will give, normal text when settled. */
@Composable
@ReadOnlyComposable
fun balanceColor(balance: Money): Color = when (balance.balanceStatus()) {
    BalanceStatus.WILL_GET -> HisaabTheme.colors.willGet
    BalanceStatus.WILL_GIVE -> HisaabTheme.colors.willGive
    BalanceStatus.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
}
