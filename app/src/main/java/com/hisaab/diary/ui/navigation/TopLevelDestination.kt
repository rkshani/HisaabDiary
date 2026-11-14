package com.hisaab.diary.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hisaab.diary.R
import kotlin.reflect.KClass

/** Tabs in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    HOME(HomeRoute, HomeRoute::class, R.drawable.ic_home, R.string.nav_home),
    REPORTS(ReportsRoute, ReportsRoute::class, R.drawable.ic_reports, R.string.nav_reports),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.drawable.ic_settings, R.string.nav_settings),
}
