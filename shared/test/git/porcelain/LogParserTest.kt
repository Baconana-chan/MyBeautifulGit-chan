package org.baconana.mybeautifulgit_chan.git.porcelain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogParserTest {
    private fun fields(vararg values: String): String = values.joinToString("\u0000")

    @Test
    fun `parses commits, parents and decorations`() {
        val raw = listOf(
            fields(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb cccccccccccccccccccccccccccccccccccccccc",
                "Anna",
                "anna@example.com",
                "1700000000",
                "Anna",
                "anna@example.com",
                "1700000001",
                "feat: interactive rebase",
                "HEAD -> main, origin/main, tag: v1",
            ),
            fields(
                "dddddddddddddddddddddddddddddddddddddddd",
                "",
                "Bob",
                "bob@example.com",
                "1690000000",
                "Bob",
                "bob@example.com",
                "1690000000",
                "init",
                "",
            ),
        ).joinToString("\n")

        val commits = LogParser.parse(raw)

        assertEquals(2, commits.size)
        with(commits[0]) {
            assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", hash)
            assertEquals("aaaaaaa", shortHash)
            assertEquals(2, parents.size)
            assertTrue(isMerge)
            assertEquals("Anna", authorName)
            assertEquals("anna@example.com", authorEmail)
            assertEquals(1700000000L, authorTime)
            assertEquals(1700000001L, committerTime)
            assertEquals("feat: interactive rebase", subject)
            assertEquals(
                listOf("HEAD -> main", "origin/main", "tag: v1"),
                refs,
            )
        }
        with(commits[1]) {
            assertTrue(parents.isEmpty())
            assertFalse(isMerge)
            assertTrue(refs.isEmpty())
        }
    }
}
