package com.hisaab.diary.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.PartyRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import com.hisaab.diary.ui.BOTTOM_BAR_TAG
import com.hisaab.diary.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// A normal phone size; the Robolectric default (320x470dp) is too small for Home.
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class NavigationTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var books: BookRepository

    @Inject lateinit var parties: PartyRepository

    @Inject lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking {
            val bookId = books.addBook("Personal")
            settings.setSelectedBook(bookId)
            settings.setOnboardingDone(true)
            parties.addParty(bookId, "Ali Raza", null, null)
        }
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithText("Ali Raza").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun startsOnHomeWithBookAndParty() {
        compose.onNode(hasText("Home") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).assertIsSelected()
        compose.onNodeWithText("Personal").assertIsDisplayed()
        compose.onNodeWithText("Ali Raza").assertIsDisplayed()
    }

    @Test
    fun bottomBarOpensReportsAndSettings() {
        compose.onNodeWithText("Reports").performClick()
        assertEquals(2, compose.onAllNodesWithText("Reports").fetchSemanticsNodes().size)

        compose.onNodeWithText("Settings").performClick()
        assertEquals(2, compose.onAllNodesWithText("Settings").fetchSemanticsNodes().size)
    }

    @Test
    fun tappingPartyOpensDetailWithoutBottomBar() {
        compose.onNodeWithText("Ali Raza").performClick()
        assertEquals(0, compose.onAllNodesWithText("Reports").fetchSemanticsNodes().size)

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Ali Raza").assertIsDisplayed()
    }

    @Test
    fun youGaveOpensNewEntry() {
        compose.onNodeWithText("Ali Raza").performClick()
        compose.onNodeWithText("You gave").performClick()
        compose.onNodeWithText("New entry", substring = true).assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
