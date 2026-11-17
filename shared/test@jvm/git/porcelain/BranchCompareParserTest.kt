package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.BranchCompareSide
import kotlin.test.Test
import kotlin.test.assertEquals

class BranchCompareParserTest {
    @Test
    fun `parses left-right counts`() {
        assertEquals(3 to 2, BranchCompareParser.parseCounts("3\t2\n"))
        assertEquals(0 to 0, BranchCompareParser.parseCounts("bad output"))
    }

    @Test
    fun `parses commits with their side`() {
        val commits = BranchCompareParser.parseCommits(
            "abc123\u0000<\u0000first change\n" +
                "def456\u0000>\u0000second change\n" +
                "ignored\u0000=\u0000boundary\n",
        )

        assertEquals(2, commits.size)
        assertEquals("abc123", commits[0].hash)
        assertEquals(BranchCompareSide.First, commits[0].side)
        assertEquals("first change", commits[0].subject)
        assertEquals(BranchCompareSide.Second, commits[1].side)
    }
}
