package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.ui.text.font.FontFamily
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeTest {

    @AfterTest
    fun reset() {
        Term.apply(ThemeMode.Dark, FontChoice.Mono)
    }

    @Test
    fun `normal and high contrast palettes keep text and borders readable`() {
        for (theme in listOf(ThemeMode.Dark, ThemeMode.Light)) {
            Term.apply(theme, FontChoice.Mono, highContrast = false)
            assertTrue(contrastRatio(Term.faint, Term.bg) >= 4.5)
            assertTrue(contrastRatio(Term.faint, Term.panelAlt) >= 4.5)
            assertTrue(contrastRatio(Term.faint, Term.raised) >= 4.5)
            assertTrue(contrastRatio(Term.borderHi, Term.panel) >= 3.0)

            Term.apply(theme, FontChoice.Mono, highContrast = true)
            assertTrue(contrastRatio(Term.faint, Term.raised) >= 4.5)
            assertTrue(contrastRatio(Term.dim, Term.raised) >= 4.5)
            assertTrue(contrastRatio(Term.border, Term.panel) >= 3.0)
        }
    }

    @Test
    fun `theme and font changes update the shared palette`() {
        Term.apply(ThemeMode.Dark, FontChoice.Mono)
        val darkBackground = Term.bg

        Term.apply(ThemeMode.Light, FontChoice.Serif)
        assertNotEquals(darkBackground, Term.bg)
        assertEquals(FontFamily.Serif, Term.mono)
        assertEquals(ThemeMode.Light, ThemeMode.parse("light"))
        assertEquals(FontChoice.Serif, FontChoice.parse("serif"))

        Term.apply(ThemeMode.System, FontChoice.Mono, systemDark = true)
        assertEquals(darkBackground, Term.bg)
        assertEquals(FontFamily.Monospace, Term.mono)

        Term.apply(ThemeMode.System, FontChoice.Mono, systemDark = false)
        assertNotEquals(darkBackground, Term.bg)
    }
}
