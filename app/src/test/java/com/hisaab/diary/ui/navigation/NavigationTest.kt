package com.hisaab.diary.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisaab.diary.ui.HisaabRoot
import com.hisaab.diary.ui.theme.HisaabTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent { HisaabTheme(dynamicColor = false) { HisaabRoot() } }
    }

    @Test
    fun startsOnHomeTab() {
        compose.onAllNodesWithText("Home")[1].assertIsSelected()
    }

    @Test
    fun bottomBarOpensReportsAndSettings() {
        compose.onNodeWithText("Reports").performClick()
        assertEquals(2, compose.onAllNodesWithText("Reports").fetchSemanticsNodes().size)

        compose.onNodeWithText("Settings").performClick()
        assertEquals(2, compose.onAllNodesWithText("Settings").fetchSemanticsNodes().size)
    }

    @Test
    fun partyScreenHidesBottomBarAndGoesBack() {
        compose.onNodeWithText("Open sample party").performClick()
        compose.onNodeWithText("Party #1").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Reports").fetchSemanticsNodes().size)

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Open sample party").assertIsDisplayed()
    }

    @Test
    fun youGaveOpensNewEntry() {
        compose.onNodeWithText("Open sample party").performClick()
        compose.onNodeWithText("You gave").performClick()
        compose.onNodeWithText("New entry").assertIsDisplayed()
    }
}
