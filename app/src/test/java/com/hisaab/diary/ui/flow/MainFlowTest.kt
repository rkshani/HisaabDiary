package com.hisaab.diary.ui.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.SettingsRepository
import com.hisaab.diary.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Main flow: add a person, add "You gave Rs 500", see the balance on the khata and Home. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var books: BookRepository

    @Inject lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking {
            settings.setSelectedBook(books.addBook("Personal"))
            settings.setOnboardingDone(true)
        }
        waitForText("Add first person")
    }

    @Test
    fun addPersonThenEntryShowsBalance() {
        compose.onNodeWithText("Add first person").performClick()
        compose.onNodeWithText("Name *").performTextInput("Ali Raza")
        compose.onNodeWithText("Save").performClick()

        // The new person's khata opens.
        waitForText("You gave")
        compose.onAllNodesWithText("You gave")[0].performClick()
        waitForText("New entry", substring = true)
        listOf("5", "0", "0").forEach { compose.onNodeWithContentDescription(it).performClick() }
        compose.onNodeWithText("Save").performClick()

        waitForText("Rs 500")
        compose.onAllNodesWithText("Rs 500")[0].assertIsDisplayed()
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
