package com.hisaab.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.hisaab.diary.ui.feature.entry.AddEntryScreen
import com.hisaab.diary.ui.feature.home.HomeRoute
import com.hisaab.diary.ui.feature.party.PartyDetailScreen
import com.hisaab.diary.ui.feature.reports.ReportsScreen
import com.hisaab.diary.ui.feature.settings.SettingsScreen

@Composable
fun HisaabNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeRoute(
                onOpenParty = { partyId -> navController.navigate(PartyDetailRoute(partyId)) },
            )
        }
        composable<ReportsRoute> { ReportsScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<PartyDetailRoute> { entry ->
            val route = entry.toRoute<PartyDetailRoute>()
            PartyDetailScreen(
                partyId = route.partyId,
                onBack = navController::popBackStack,
                onAddEntry = { isGave -> navController.navigate(AddEntryRoute(route.partyId, isGave)) },
            )
        }
        composable<AddEntryRoute> {
            AddEntryScreen(onBack = navController::popBackStack)
        }
    }
}
