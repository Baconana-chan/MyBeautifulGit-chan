package org.baconana.mybeautifulgit_chan.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OperationProgressTest {

    @Test
    fun `parses local and remote progress lines`() {
        assertEquals(
            OperationProgress("Receiving objects", 42, "42/100"),
            parseOperationProgress("remote: Receiving objects:  42% (42/100), 1.2 MiB\r"),
        )
        assertEquals(
            OperationProgress("Resolving deltas", 100, "100/100"),
            parseOperationProgress("Resolving deltas: 100% (100/100), done.\n"),
        )
    }

    @Test
    fun `uses the last progress line from a chunk`() {
        val progress = parseOperationProgress(
            "Enumerating objects: 10% (1/10)\rReceiving objects: 75% (75/100)\r",
        )
        assertEquals(OperationProgress("Receiving objects", 75, "75/100"), progress)
    }

    @Test
    fun `ignores ordinary stderr`() {
        assertNull(parseOperationProgress("fatal: could not read from remote repository\n"))
    }
}