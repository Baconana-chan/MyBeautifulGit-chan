package org.baconana.mybeautifulgit_chan.git.porcelain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrepParserTest {

    @Test
    fun `parses multiple paths and lines`() {
        val matches = GrepParser.parse(
            "src/a:b.kt\u000012\u0000first hit\n" +
                "src/a:b.kt\u000015\u0000 second hit\n" +
                "README.md\u00003\u0000hit again\n",
        )

        assertEquals(3, matches.size)
        assertEquals("src/a:b.kt", matches[0].path)
        assertEquals(12, matches[0].lineNumber)
        assertEquals("first hit", matches[0].text)
        assertEquals(15, matches[1].lineNumber)
        assertEquals(" second hit", matches[1].text)
        assertEquals("README.md", matches[2].path)
        assertEquals("hit again", matches[2].text)
    }

    @Test
    fun `empty and malformed output has no matches`() {
        assertTrue(GrepParser.parse("").isEmpty())
        assertTrue(GrepParser.parse("only-a-path").isEmpty())
    }
}
