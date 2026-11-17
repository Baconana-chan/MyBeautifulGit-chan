package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorktreesTest {

    @Test
    fun `separates remove lock and unlock paths from options`() {
        assertEquals(
            listOf("worktree", "remove", "--", "-linked"),
            worktreeRemoveArgs("-linked", force = false),
        )
        assertEquals(
            listOf("worktree", "remove", "--force", "--", "-linked"),
            worktreeRemoveArgs("-linked", force = true),
        )
        assertEquals(
            listOf("worktree", "lock", "--", "-linked"),
            worktreeLockArgs("-linked"),
        )
        assertEquals(
            listOf(
                "worktree",
                "lock",
                "--reason",
                "test lock",
                "--",
                "-linked",
            ),
            worktreeLockArgs("-linked", "test lock"),
        )
        assertEquals(
            listOf("worktree", "unlock", "--", "-linked"),
            worktreeUnlockArgs("-linked"),
        )
    }

    @Test
    fun `validates worktree paths`() {
        assertTrue(validWorktreePath("linked"))
        assertTrue(validWorktreePath("nested/linked"))
        assertTrue(validWorktreePath("/tmp/linked"))
        assertFalse(validWorktreePath(""))
        assertFalse(validWorktreePath("-linked"))
        assertFalse(validWorktreePath("linked\nnext"))
    }
}
