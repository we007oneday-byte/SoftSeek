package io.github.we007oneday_byte.softseek.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {
    @Test
    fun cycleWrapsThroughThreeModes() {
        assertEquals(ThemeMode.SYSTEM, ThemeManager.mode)

        ThemeManager.cycle()
        assertEquals(ThemeMode.LIGHT, ThemeManager.mode)

        ThemeManager.cycle()
        assertEquals(ThemeMode.DARK, ThemeManager.mode)

        ThemeManager.cycle()
        assertEquals(ThemeMode.SYSTEM, ThemeManager.mode)
    }
}
