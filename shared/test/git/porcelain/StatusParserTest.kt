package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.StatusKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatusParserTest {
    private fun record(vararg fields: String): String = fields.joinToString("\u0000") + "\u0000"

    @Test
    fun `parses branch, changed, renamed, conflicted and untracked entries`() {
        val oid = "1111111111111111111111111111111111111111"
        val raw = buildString {
            append(record("# branch.oid $oid"))
            append(record("# branch.head main"))
            append(record("# branch.upstream origin/main"))
            append(record("# branch.ab +2 -3"))
            append(record("1 .M N... 100644 100644 100644 aaaa bbbb src/App.kt"))
            append(record("1 M. N... 100644 100644 100644 cccc dddd staged.kt"))
            append(record("2 R. N... 100644 100644 100644 eeee ffff R100 new name.kt"))
            append("old name.kt\u0000")
            append(record("u UU N... 100644 100644 100644 100644 1111 2222 3333 conflict.kt"))
            append(record("? untracked.txt"))
            append(record("! build/"))
        }

        val status = StatusParser.parse(raw)

        assertEquals("main", status.branch?.head)
        assertEquals(oid, status.branch?.oid)
        assertEquals("origin/main", status.branch?.upstream)
        assertEquals(2, status.branch?.ahead)
        assertEquals(3, status.branch?.behind)
        assertFalse(status.branch?.detached ?: true)
        assertFalse(status.branch?.unborn ?: true)

        assertEquals(6, status.entries.size)
        assertEquals(2, status.stagedCount)
        assertEquals(2, status.unstagedCount)
        assertEquals(1, status.untrackedCount)
        assertEquals(1, status.conflictedCount)
        assertFalse(status.isClean)

        val modified = status.entries.single { it.path == "src/App.kt" }
        assertEquals(StatusKind.Changed, modified.kind)
        assertEquals('.', modified.indexStatus)
        assertEquals('M', modified.workTreeStatus)
        assertFalse(modified.isStaged)
        assertTrue(modified.isUnstaged)

        val staged = status.entries.single { it.path == "staged.kt" }
        assertTrue(staged.isStaged)
        assertFalse(staged.isUnstaged)

        val renamed = status.entries.single { it.kind == StatusKind.Renamed }
        assertEquals("new name.kt", renamed.path)
        assertEquals("old name.kt", renamed.originalPath)
        assertTrue(renamed.isStaged)

        val conflicted = status.entries.single { it.kind == StatusKind.Unmerged }
        assertTrue(conflicted.isConflicted)
        assertFalse(conflicted.isStaged)
        assertFalse(conflicted.isUnstaged)
        assertEquals(3, conflicted.conflictStages.size)
        assertEquals("2222", conflicted.conflictStages[1].objectId)
    }

    @Test
    fun `detects detached head and unborn branch`() {
        val raw = buildString {
            append(record("# branch.oid (initial)"))
            append(record("# branch.head (detached)"))
        }

        val branch = StatusParser.parse(raw).branch

        assertNull(branch?.head)
        assertNull(branch?.oid)
        assertTrue(branch?.detached ?: false)
        assertTrue(branch?.unborn ?: false)
    }

    @Test
    fun `ignored files do not make the tree dirty`() {
        val raw = buildString {
            append(record("# branch.oid abc"))
            append(record("# branch.head main"))
            append(record("! build/"))
        }

        val status = StatusParser.parse(raw)

        assertEquals(1, status.entries.size)
        assertEquals(StatusKind.Ignored, status.entries.single().kind)
        assertEquals(1, status.ignoredCount)
        assertTrue(status.isClean)
    }

    @Test
    fun `parses submodule status flags`() {
        val raw = record("1 .M S.M. 160000 160000 160000 aaaa bbbb libs/foo")

        val entry = StatusParser.parse(raw).entries.single()

        assertEquals(StatusKind.Changed, entry.kind)
        assertEquals("libs/foo", entry.path)
        assertEquals("S.M.", entry.submodule)
        assertTrue(entry.isSubmodule)
        assertTrue(entry.submoduleDirty)
        assertFalse(entry.isStaged)
        assertTrue(entry.isUnstaged)
    }
}
