package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.ui.graphics.toArgb
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualStyleSpecTest {
    @Test
    fun `visual styles follow the fixed task book order`() {
        assertEquals(
            listOf(
                "Minimal",
                "Cloud",
                "Pudding",
                "Mono",
                "Taiji",
                "Hacker",
                "LineArt",
                "Candy",
                "Neon",
                "PaperInk",
            ),
            VisualStyle.entries.map { it.name },
        )
    }

    @Test
    fun `all themes use the same fixed error color`() {
        VisualStyle.entries.forEach { style ->
            val spec = visualStyleSpec(style)
            assertEquals(0xFFE53935.toInt(), spec.light.error.toArgb())
            assertEquals(0xFFE53935.toInt(), spec.dark.error.toArgb())
        }
    }

    @Test
    fun `only hacker and neon force dark mode`() {
        val forced = VisualStyle.entries.filter { visualStyleSpec(it).forceDarkMode }
        assertEquals(listOf(VisualStyle.Hacker, VisualStyle.Neon), forced)
    }

    @Test
    fun `line art uses stroke only cards bubbles and buttons`() {
        val spec = visualStyleSpec(VisualStyle.LineArt)
        assertEquals(StyleSurfacePaint.StrokeOnly, spec.cardPaint)
        assertEquals(StyleSurfacePaint.StrokeOnly, spec.bubblePaint)
        assertEquals(StyleButtonPaint.StrokeOnly, spec.buttonPaint)
    }

    @Test
    fun `legacy uppercase style names are accepted`() {
        assertEquals(VisualStyle.Minimal, VisualStyle.fromName("MINIMAL"))
        assertEquals(VisualStyle.LineArt, VisualStyle.fromName("LINE"))
        assertEquals(VisualStyle.PaperInk, VisualStyle.fromName("PAPER_INK"))
        assertEquals(VisualStyle.Minimal, VisualStyle.fromName("REMOVED_STYLE"))
    }

    @Test
    fun `paper ink keeps wide screen margins`() {
        assertTrue(visualStyleSpec(VisualStyle.PaperInk).layout.screenHorizontalPaddingDp >= 24)
        assertFalse(visualStyleSpec(VisualStyle.Minimal).forceDarkMode)
    }
}
