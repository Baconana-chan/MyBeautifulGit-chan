package org.baconana.mybeautifulgit_chan.git.porcelain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlameParserTest {

    @Test
    fun `parses commit metadata and line contents`() {
        val lines = BlameParser.parse(
            "abc123abc123abc123abc123abc123abc123abcd 1 1 1\n" +
                "author Alice\n" +
                "author-mail <alice@example.com>\n" +
                "author-time 1700000000\n" +
                "author-tz +0000\n" +
                "summary first change\n" +
                "boundary\n" +
                "filename a.txt\n" +
                "\t  leading text\n" +
                "def456def456def456def456def456def456def4 2 2 1\n" +
                "author Bob\n" +
                "author-mail <bob@example.com>\n" +
                "author-time 1700000100\n" +
                "summary second change\n" +
                "filename b.txt\n" +
                "\tsecond line\n",
        )

        assertEquals(2, lines.size)
        assertEquals("abc123abc123abc123abc123abc123abc123abcd", lines[0].hash)
        assertEquals(1, lines[0].lineNumber)
        assertEquals("Alice", lines[0].authorName)
        assertEquals("alice@example.com", lines[0].authorEmail)
        assertEquals(1_700_000_000L, lines[0].authorTime)
        assertEquals("first change", lines[0].summary)
        assertEquals("  leading text", lines[0].text)
        assertEquals(2, lines[1].lineNumber)
        assertEquals("second line", lines[1].text)
    }

    @Test
    fun `ignores malformed blocks without content`() {
        assertTrue(BlameParser.parse("not a blame header\nauthor Alice\n").isEmpty())
    }
}
