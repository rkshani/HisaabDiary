package com.hisaab.diary.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisaab.diary.ui.HisaabRoot
import com.hisaab.diary.ui.theme.HisaabTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RtlLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomBarIsMirroredInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HisaabTheme(dynamicColor = false) { HisaabRoot() }
            }
        }
        compose.onNodeWithText("Settings").assertIsDisplayed()

        compose.onNodeWithText("Open sample party").assertIsDisplayed()
        val settingsLeft = compose.onNodeWithText("Settings").getUnclippedBoundsInRoot().left
        val reportsLeft = compose.onNodeWithText("Reports").getUnclippedBoundsInRoot().left
        // In RTL the first tab (Home) is on the right, so Settings is left of Reports.
        assertTrue(settingsLeft < reportsLeft)
    }
}
