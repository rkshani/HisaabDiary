package com.hisaab.diary.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hisaab.diary.R
import com.hisaab.diary.ui.components.PRIVACY_POLICY_URL
import com.hisaab.diary.ui.components.openPlayStore
import com.hisaab.diary.ui.components.openWebPage
import com.hisaab.diary.ui.navigation.HisaabNavHost
import com.hisaab.diary.ui.navigation.TopLevelDestination
import kotlinx.coroutines.launch

/** App shell: side drawer (opened from Home), bottom bar on the 3 main tabs, full screen everywhere else. */
@Composable
fun HisaabRoot(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { destination.isOn(it) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe to open only on Home; any open drawer can always be swiped closed.
        gesturesEnabled = drawerState.isOpen || destination.isOn(TopLevelDestination.HOME),
        modifier = modifier,
        drawerContent = {
            AppDrawerContent(
                onItemClick = { item ->
                    scope.launch { drawerState.close() }
                    when (item) {
                        DrawerItem.HOME -> navController.navigateToTab(TopLevelDestination.HOME)
                        DrawerItem.RATE -> if (!openPlayStore(context)) showLinkError(context)
                        DrawerItem.PRIVACY -> if (!openWebPage(context, PRIVACY_POLICY_URL)) showLinkError(context)
                        DrawerItem.EXIT -> activity?.finish()
                    }
                },
            )
        },
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    HisaabBottomBar(
                        currentDestination = destination,
                        onSelect = { navController.navigateToTab(it) },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .consumeWindowInsets(padding),
            ) {
                HisaabNavHost(
                    navController = navController,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                )
            }
        }
    }
}

const val BOTTOM_BAR_TAG = "bottom_bar"

private fun showLinkError(context: Context) {
    Toast.makeText(context, R.string.link_open_failed, Toast.LENGTH_SHORT).show()
}

@Composable
private fun HisaabBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.testTag(BOTTOM_BAR_TAG)) {
        TopLevelDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentDestination.isOn(tab),
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
