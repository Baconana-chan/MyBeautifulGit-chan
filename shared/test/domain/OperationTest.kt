package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OperationTest {

    @Test
    fun `maps continue abort and skip for supported operations`() {
        val expected = mapOf(
            GitOperationKind.Rebase to "rebase",
            GitOperationKind.CherryPick to "cherry-pick",
            GitOperationKind.Revert to "revert",
            GitOperationKind.Am to "am",
        )

        expected.forEach { (kind, command) ->
            assertEquals(listOf(command, "--continue"), kind.operationArgs(OperationAction.Continue))
            assertEquals(listOf(command, "--abort"), kind.operationArgs(OperationAction.Abort))
            assertEquals(listOf(command, "--skip"), kind.operationArgs(OperationAction.Skip))
        }
    }

    @Test
    fun `merge supports continue and abort but not skip`() {
        assertEquals(
            listOf("merge", "--continue"),
            GitOperationKind.Merge.operationArgs(OperationAction.Continue),
        )
        assertEquals(
            listOf("merge", "--abort"),
            GitOperationKind.Merge.operationArgs(OperationAction.Abort),
        )
        assertNull(GitOperationKind.Merge.operationArgs(OperationAction.Skip))
    }

    @Test
    fun `bisect has no operation actions`() {
        OperationAction.entries.forEach { action ->
            assertNull(GitOperationKind.Bisect.operationArgs(action))
        }
    }
}
