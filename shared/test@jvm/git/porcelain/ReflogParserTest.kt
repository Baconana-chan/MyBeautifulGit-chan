package org.baconana.mybeautifulgit_chan.git.porcelain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReflogParserTest {

    @Test
    fun `parses selectors hashes subjects and timestamps`() {
        val entries = ReflogParser.parse(
            "abc123\u0000HEAD@{0}\u0000commit: second\u00001794663000\n" +
                "def456\u0000refs/heads/main@{1}\u0000checkout: moving from main\u00001794662900\n" +
                "broken\n",
        )

        assertEquals(2, entries.size)
        assertEquals("abc123", entries[0].hash)
        assertEquals("HEAD@{0}", entries[0].selector)
        assertEquals("commit: second", entries[0].subject)
        assertEquals(1_794_663_000L, entries[0].time)
        assertEquals("refs/heads/main@{1}", entries[1].selector)
    }

    @Test
    fun `drops entries without a timestamp`() {
        assertTrue(ReflogParser.parse("abc\u0000HEAD@{0}\u0000message\u0000not-a-number\n").isEmpty())
    }
}
