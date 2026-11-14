package com.hisaab.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hisaab.diary.ui.feature.entry.AddEntryDestination
import com.hisaab.diary.ui.feature.home.HomeRoute
import com.hisaab.diary.ui.feature.party.PartyDetailDestination
import com.hisaab.diary.ui.feature.reminders.RemindersDestination
import com.hisaab.diary.ui.feature.reports.ReportsDestination
import com.hisaab.diary.ui.feature.settings.SettingsDestination

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
                onOpenReminders = { navController.navigate(RemindersRoute) },
            )
        }
        composable<RemindersRoute> {
            RemindersDestination(
                onBack = navController::popBackStack,
                onOpenParty = { partyId -> navController.navigate(PartyDetailRoute(partyId)) },
            )
        }
        composable<ReportsRoute> { ReportsDestination() }
        composable<SettingsRoute> { SettingsDestination() }
        composable<PartyDetailRoute> {
            PartyDetailDestination(
                onBack = navController::popBackStack,
                onAddEntry = { partyId, isGave -> navController.navigate(AddEntryRoute(partyId, isGave)) },
                onOpenEntry = { partyId, entryId -> navController.navigate(AddEntryRoute(partyId, entryId = entryId)) },
            )
        }
        composable<AddEntryRoute> {
            AddEntryDestination(onBack = navController::popBackStack)
        }
    }
}
