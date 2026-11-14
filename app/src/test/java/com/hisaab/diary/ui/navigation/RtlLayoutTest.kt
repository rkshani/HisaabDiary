package com.hisaab.diary.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.hisaab.diary.HiltTestActivity
import com.hisaab.diary.ui.HisaabRoot
import com.hisaab.diary.ui.theme.HisaabTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class RtlLayoutTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Test
    fun bottomBarIsMirroredInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HisaabTheme(dynamicColor = false) { HisaabRoot() }
            }
        }
        val settingsLeft = compose.onNodeWithText("Settings").getUnclippedBoundsInRoot().left
        val reportsLeft = compose.onNodeWithText("Reports").getUnclippedBoundsInRoot().left
        // In RTL the first tab (Home) is on the right, so Settings is left of Reports.
        assertTrue(settingsLeft < reportsLeft)
    }
}
