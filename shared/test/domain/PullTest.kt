package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PullTest {
    @Test
    fun `always spells out the pull mode`() {
        assertEquals(
            listOf("pull", "--no-rebase", "--no-edit", "origin", "main"),
            pullArgs("origin", "main", PullMode.Merge),
        )
        assertEquals(
            listOf("pull", "--rebase", "origin", "main"),
            pullArgs("origin", "main", PullMode.Rebase),
        )
        assertEquals(
            listOf("pull", "--ff-only", "origin", "main"),
            pullArgs("origin", "main", PullMode.FfOnly),
        )
    }

    @Test
    fun `signature verification is explicit and merge-only`() {
        assertEquals(
            listOf("pull", "--no-rebase", "--no-edit", "--verify-signatures", "origin", "main"),
            pullArgs("origin", "main", PullMode.Merge, verifySignatures = true),
        )
        assertEquals(
            listOf("pull", "--rebase", "origin", "main"),
            pullArgs("origin", "main", PullMode.Rebase, verifySignatures = true),
        )
    }
}
