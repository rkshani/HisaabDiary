package com.hisaab.diary.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hisaab.diary.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class NavigationTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsOnHomeTab() {
        compose.onNodeWithText("Home").assertIsSelected()
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
