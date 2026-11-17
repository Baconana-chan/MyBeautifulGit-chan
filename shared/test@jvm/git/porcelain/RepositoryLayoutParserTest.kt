package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.SubmoduleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorktreeParserTest {

    @Test
    fun `parses multiple worktrees including bare and detached`() {
        val raw = """
            worktree C:/repo
            HEAD aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            branch refs/heads/main

            worktree C:/worktrees/feature
            HEAD bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
            detached

            worktree C:/bare
            bare
        """.trimIndent()

        val worktrees = WorktreeParser.parse(raw)

        assertEquals(3, worktrees.size)
        assertEquals("main", worktrees[0].branchName)
        assertEquals("refs/heads/main", worktrees[0].branch)
        assertFalse(worktrees[0].isDetached)
        assertFalse(worktrees[0].isBare)

        assertTrue(worktrees[1].isDetached)
        assertNull(worktrees[1].branchName)
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", worktrees[1].head)

        assertTrue(worktrees[2].isBare)
        assertNull(worktrees[2].head)
    }

    @Test
    fun `parses locked and prunable reasons and paths with spaces`() {
        val raw = """
            worktree C:/my repo
            HEAD cccccccccccccccccccccccccccccccccccccccc
            branch refs/heads/main
            locked user is editing
            prunable gitdir file points to non-existent location
        """.trimIndent()

        val worktree = WorktreeParser.parse(raw).single()

        assertEquals("C:/my repo", worktree.path)
        assertTrue(worktree.isLocked)
        assertEquals("user is editing", worktree.lockReason)
        assertTrue(worktree.isPrunable)
        assertEquals("gitdir file points to non-existent location", worktree.prunableReason)
    }

    @Test
    fun `ignores incomplete or empty records`() {
        assertTrue(WorktreeParser.parse("").isEmpty())
        assertTrue(WorktreeParser.parse("HEAD abc\nbranch refs/heads/main").isEmpty())
    }
}

class SubmoduleParserTest {

    @Test
    fun `parses all submodule states and descriptions`() {
        val raw = """
             1111111111111111111111111111111111111111 libs/current (v1.0)
            +2222222222222222222222222222222222222222 libs/modified
            -3333333333333333333333333333333333333333 libs/uninitialized
            U4444444444444444444444444444444444444444 libs/conflict (heads/main)
        """.trimIndent()

        val submodules = SubmoduleParser.parse(raw)

        assertEquals(4, submodules.size)
        assertEquals(SubmoduleState.Current, submodules[0].state)
        assertEquals("libs/current", submodules[0].path)
        assertEquals("v1.0", submodules[0].describe)
        assertEquals(SubmoduleState.Modified, submodules[1].state)
        assertNull(submodules[1].describe)
        assertEquals(SubmoduleState.Uninitialized, submodules[2].state)
        assertEquals(SubmoduleState.Conflict, submodules[3].state)
        assertEquals("heads/main", submodules[3].describe)
    }

    @Test
    fun `supports nested paths and sha256 object ids`() {
        val oid = "a".repeat(64)
        val raw = " $oid libs/nested/module (heads/main)"

        val submodule = SubmoduleParser.parse(raw).single()

        assertEquals("libs/nested/module", submodule.path)
        assertEquals(oid, submodule.head)
    }

    @Test
    fun `ignores malformed lines`() {
        assertTrue(SubmoduleParser.parse("").isEmpty())
        assertTrue(SubmoduleParser.parse("not a submodule status").isEmpty())
        assertTrue(SubmoduleParser.parse("?1234567 path").isEmpty())
    }
}
