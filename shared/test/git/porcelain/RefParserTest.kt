package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.RefKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefParserTest {
    private fun record(vararg fields: String): String = fields.joinToString("\u0000")

    @Test
    fun `parses local, remote, symbolic branches and tags`() {
        val raw = listOf(
            record(
                "refs/heads/main",
                "aaaaaaaa",
                "commit",
                "",
                "refs/remotes/origin/main",
                "[ahead 2, behind 3]",
                "*",
            ),
            record(
                "refs/remotes/origin/main",
                "aaaaaaaa",
                "commit",
                "",
                "",
                "",
                "",
            ),
            record(
                "refs/remotes/origin/HEAD",
                "aaaaaaaa",
                "commit",
                "refs/remotes/origin/main",
                "",
                "",
                "",
            ),
            record(
                "refs/tags/v1.0.0",
                "bbbbbbbb",
                "tag",
                "",
                "",
                "",
                "",
            ),
        ).joinToString("\n")

        val refs = RefParser.parse(raw)

        assertEquals(4, refs.size)
        with(refs[0]) {
            assertEquals(RefKind.LocalBranch, kind)
            assertEquals("main", shortName)
            assertTrue(isHead)
            assertEquals("refs/remotes/origin/main", upstream)
            assertEquals(2, ahead)
            assertEquals(3, behind)
            assertFalse(upstreamGone)
        }
        assertEquals(RefKind.RemoteBranch, refs[1].kind)
        assertEquals("origin/main", refs[1].shortName)
        assertEquals("origin/HEAD", refs[2].shortName)
        assertTrue(refs[2].isSymbolic)
        assertEquals("refs/remotes/origin/main", refs[2].symbolicTarget)
        assertNull(refs[2].upstream)
        assertEquals("v1.0.0", refs[3].shortName)
        assertEquals(RefKind.Tag, refs[3].kind)
            }

    @Test
    fun `marks gone upstream`() {
        val raw = record(
            "refs/heads/feature",
            "cccccccc",
            "commit",
            "",
            "refs/remotes/origin/feature",
            "[gone]",
            " ",
        )

        val ref = RefParser.parse(raw).single()

        assertTrue(ref.upstreamGone)
        assertEquals(0, ref.ahead)
        assertEquals(0, ref.behind)
    }
}
