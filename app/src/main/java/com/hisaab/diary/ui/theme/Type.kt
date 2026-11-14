package com.hisaab.diary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val Default = Typography()

internal val HisaabTypography = Typography(
    displaySmall = Default.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Style for money amounts: fixed-width (tabular) digits so amounts line up in lists. */
fun TextStyle.amount(): TextStyle = copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)
