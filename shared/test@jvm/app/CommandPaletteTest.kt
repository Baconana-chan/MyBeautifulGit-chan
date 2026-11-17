package org.baconana.mybeautifulgit_chan.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandPaletteTest {
    private fun command(
        id: String,
        title: String,
        category: String = "TEST",
        description: String = "",
        command: String = "",
        keywords: String = "",
    ) = PaletteCommand(
        id = id,
        title = title,
        category = category,
        description = description,
        command = command,
        keywords = keywords,
    ) {}

    @Test
    fun `empty query preserves registry order`() {
        val commands = listOf(
            command("one", "First"),
            command("two", "Second"),
            command("three", "Third"),
        )

        assertEquals(listOf("one", "two", "three"), filterPaletteCommands(commands, "").map { it.id })
        assertEquals(listOf("one", "two", "three"), filterPaletteCommands(commands, "  ").map { it.id })
    }

    @Test
    fun `finds commands by git command and description`() {
        val commands = listOf(
            command("cherry", "Apply change", command = "git cherry-pick <commit>"),
            command("gc", "Maintenance", description = "Aggressive garbage collection", command = "git gc"),
            command("branch", "Branches", command = "git branch"),
        )

        assertEquals(listOf("cherry"), filterPaletteCommands(commands, "cherry-pick").map { it.id })
        assertEquals(listOf("gc"), filterPaletteCommands(commands, "garbage").map { it.id })
    }

    @Test
    fun `matches keywords case-insensitively and across several tokens`() {
        val commands = listOf(
            command(
                id = "reset",
                title = "Reset to commit",
                description = "Choose soft, mixed or hard reset",
                command = "git reset --mixed <commit>",
                keywords = "history rewind",
            ),
            command("push", "Push", command = "git push"),
        )

        assertEquals(listOf("reset"), filterPaletteCommands(commands, "HARD REWIND").map { it.id })
        assertEquals(listOf("reset"), filterPaletteCommands(commands, "git reset").map { it.id })
    }

    @Test
    fun `title prefix outranks a description match`() {
        val commands = listOf(
            command("desc", "Something else", description = "push the current branch"),
            command("title", "Push changes", command = "git push"),
        )

        val result = filterPaletteCommands(commands, "push")
        assertEquals("title", result.first().id)
        assertTrue(result.any { it.id == "desc" })
    }

    @Test
    fun `screen commands can only be cleared by their owner`() {
        val palette = CommandPaletteController()
        val first = Any()
        val second = Any()

        palette.setScreenCommands(first, listOf(command("first", "First")))
        palette.setScreenCommands(second, listOf(command("second", "Second")))
        palette.clearScreenCommands(first)

        assertEquals(listOf("second"), palette.commands.map { it.id })

        palette.clearScreenCommands(second)
        assertTrue(palette.commands.isEmpty())
    }
}
