package com.hisaab.diary.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun `add entry defaults to new gave entry`() {
        val route = AddEntryRoute(partyId = 5)
        assertTrue(route.isGave)
        assertNull(route.entryId)
    }

    @Test
    fun `there are three bottom tabs in order`() {
        assertEquals(
            listOf(HomeRoute, ReportsRoute, SettingsRoute),
            TopLevelDestination.entries.map { it.route },
        )
    }
}
