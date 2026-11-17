package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RebaseTodoTest {

    @Test
    fun `parses every interactive rebase command`() {
        val text = """
            # comment
            pick aaa111 base
            reword bbb222 second subject
            edit ccc333 third
            squash ddd444 fourth
            fixup eee555 fifth
            drop fff666 sixth
            exec make test
            break
            label onto
            reset onto # base
            merge -C abc123 feature # merge feature
        """.trimIndent()

        val entries = RebaseTodo.parse(text)

        assertEquals(
            listOf(
                RebaseCommand.Pick,
                RebaseCommand.Reword,
                RebaseCommand.Edit,
                RebaseCommand.Squash,
                RebaseCommand.Fixup,
                RebaseCommand.Drop,
                RebaseCommand.Exec,
                RebaseCommand.Break,
                RebaseCommand.Label,
                RebaseCommand.Reset,
                RebaseCommand.Merge,
            ),
            entries.map { it.command },
        )
        assertEquals("aaa111", entries[0].argument)
        assertEquals("base", entries[0].comment)
        assertEquals("make test", entries[6].argument)
        assertEquals("onto", entries[8].argument)
        assertEquals("onto # base", entries[9].argument)
        assertEquals("-C abc123 feature # merge feature", entries[10].argument)
        assertEquals("", entries[10].comment)
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), entries.map { it.id })
    }

    @Test
    fun `parses fixup with message source option`() {
        val entries = RebaseTodo.parse(
            """
                fixup -C abc123 # amend! subject
                fixup -c def456 source
            """.trimIndent(),
        )

        assertEquals(2, entries.size)
        assertEquals("-C abc123", entries[0].argument)
        assertEquals("abc123", entries[0].hash)
        assertEquals("amend! subject", entries[0].comment)
        assertEquals("-c def456", entries[1].argument)
        assertEquals("def456", entries[1].hash)
        assertEquals("source", entries[1].comment)
    }

    @Test
    fun `renders a round-trippable todo and validates required arguments`() {
        val entries = listOf(
            RebaseTodoEntry(1, RebaseCommand.Pick, "abc123", "subject"),
            RebaseTodoEntry(2, RebaseCommand.Exec, "make test"),
            RebaseTodoEntry(3, RebaseCommand.Break),
            RebaseTodoEntry(4, RebaseCommand.Merge, "-C def456 feature", "merge"),
        )

        val rendered = RebaseTodo.render(entries)
        val parsed = RebaseTodo.parse(rendered)

        assertEquals(entries.map { it.command }, parsed.map { it.command })
        assertEquals(entries.map { it.argument }, parsed.map { it.argument })
        assertNull(RebaseTodo.validate(entries))
        assertEquals(
            RebaseTodoProblem.MissingExec,
            RebaseTodo.validate(listOf(RebaseTodoEntry(1, RebaseCommand.Exec))),
        )
        assertEquals(
            RebaseTodoProblem.MissingMessage,
            RebaseTodo.validate(
                listOf(RebaseTodoEntry(1, RebaseCommand.Reword, "abc123", "subject")),
            ),
        )
        assertEquals(RebaseTodoProblem.Empty, RebaseTodo.validate(emptyList()))
        assertTrue(RebaseTodo.render(emptyList()).isBlank())
    }
}
