package com.hisaab.diary.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun `initials from names`() {
        assertEquals("AR", initialsOf("Ali Raza"))
        assertEquals("A", initialsOf("ali"))
        assertEquals("MA", initialsOf("  Muhammad   Ahmed  Khan "))
        assertEquals("?", initialsOf("   "))
        assertEquals("1S", initialsOf("1st Shop"))
    }
}
