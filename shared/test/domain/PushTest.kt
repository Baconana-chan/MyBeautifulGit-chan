package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PushTest {
    @Test
    fun `builds current and another branch pushes`() {
        assertEquals(
            listOf("push", "--set-upstream", "origin", "main"),
            pushArgs(
                "origin",
                PushOptions(mode = PushMode.CurrentBranch, source = "main"),
            ),
        )
        assertEquals(
            listOf(
                "push",
                "--force-with-lease",
                "origin",
                "main:release",
            ),
            pushArgs(
                "origin",
                PushOptions(
                    mode = PushMode.AnotherBranch,
                    source = "main",
                    destination = "release",
                    force = PushForce.Lease,
                ),
            ),
        )
    }

    @Test
    fun `builds bulk modes and delete`() {
        assertEquals(
            listOf("push", "origin", "--tags"),
            pushArgs("origin", PushOptions(mode = PushMode.AllTags)),
        )
        assertEquals(
            listOf("push", "origin", "--all"),
            pushArgs("origin", PushOptions(mode = PushMode.AllBranches)),
        )
        assertEquals(
            listOf("push", "--force", "origin", "--mirror"),
            pushArgs(
                "origin",
                PushOptions(mode = PushMode.Mirror, force = PushForce.Force),
            ),
        )
        assertEquals(
            listOf("push", "origin", "--delete", "old"),
            pushArgs(
                "origin",
                PushOptions(
                    mode = PushMode.DeleteBranch,
                    branch = "old",
                    force = PushForce.Force,
                ),
            ),
        )
    }

    @Test
    fun `keeps arbitrary refspec unchanged`() {
        assertEquals(
            listOf("push", "origin", "refs/heads/main:refs/heads/custom"),
            pushArgs(
                "origin",
                PushOptions(
                    mode = PushMode.Refspec,
                    refspec = "refs/heads/main:refs/heads/custom",
                ),
            ),
        )
        assertTrue(validPushRefspec("main:release"))
        assertTrue(validPushRefspec("HEAD:refs/heads/feature"))
        assertFalse(validPushRefspec(""))
        assertFalse(validPushRefspec("-main"))
        assertFalse(validPushRefspec("main:bad name"))
    }
}
