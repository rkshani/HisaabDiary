package com.hisaab.diary.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Money colors that Material 3 does not have.
 * They stay the same with dynamic color, so green always means "you will get".
 */
@Immutable
data class HisaabColors(
    val willGet: Color,
    val onWillGet: Color,
    val willGetContainer: Color,
    val onWillGetContainer: Color,
    val willGive: Color,
    val onWillGive: Color,
    val willGiveContainer: Color,
    val onWillGiveContainer: Color,
)

internal val LightHisaabColors = HisaabColors(
    willGet = GetGreen40,
    onWillGet = Color.White,
    willGetContainer = GetGreen90,
    onWillGetContainer = GetGreen10,
    willGive = GiveRed40,
    onWillGive = Color.White,
    willGiveContainer = GiveRed90,
    onWillGiveContainer = GiveRed10,
)

internal val DarkHisaabColors = HisaabColors(
    willGet = GetGreen80,
    onWillGet = GetGreen10,
    willGetContainer = GetGreen30,
    onWillGetContainer = GetGreen90,
    willGive = GiveRed80,
    onWillGive = GiveRed10,
    willGiveContainer = GiveRed30,
    onWillGiveContainer = GiveRed90,
)

val LocalHisaabColors = staticCompositionLocalOf { LightHisaabColors }
