package com.hisaab.diary.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorsTest {
    @Test
    fun `light brand primary is teal`() {
        assertEquals(Teal40, brandColorScheme(darkTheme = false).primary)
    }

    @Test
    fun `dark brand scheme has a dark background`() {
        assertTrue(brandColorScheme(darkTheme = true).background.luminance() < 0.1f)
    }

    @Test
    fun `money colors pass WCAG AA contrast on surface`() {
        val light = brandColorScheme(darkTheme = false).surface
        val dark = brandColorScheme(darkTheme = true).surface
        listOf(LightHisaabColors.willGet, LightHisaabColors.willGive).forEach {
            assertTrue("contrast too low for $it", contrast(it, light) >= MIN_CONTRAST)
        }
        listOf(DarkHisaabColors.willGet, DarkHisaabColors.willGive).forEach {
            assertTrue("contrast too low for $it", contrast(it, dark) >= MIN_CONTRAST)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return max(la, lb) / min(la, lb)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
