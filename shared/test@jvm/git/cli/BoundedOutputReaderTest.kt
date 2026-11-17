package org.baconana.mybeautifulgit_chan.git.cli

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundedOutputReaderTest {

    @Test
    fun `retains the limit drains the rest and signals once`() {
        val source = ByteArray(4_096) { 42 }
        var signals = 0
        val reader = BoundedOutputReader(
            input = ByteArrayInputStream(source),
            limit = 1_024,
            onLimit = { signals++ },
        )

        reader.run()

        assertTrue(reader.truncated)
        assertEquals(1_024, reader.bytes().size)
        assertEquals(1, signals)
    }

    @Test
    fun `does not truncate output below the limit`() {
        val source = ByteArray(512) { 7 }
        val reader = BoundedOutputReader(
            input = ByteArrayInputStream(source),
            limit = 1_024,
        )

        reader.run()

        assertFalse(reader.truncated)
        assertEquals(512, reader.bytes().size)
    }
}
