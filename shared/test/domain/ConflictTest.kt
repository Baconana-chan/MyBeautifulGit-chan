package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConflictTest {

    @Test
    fun `parses standard ours and theirs conflict`() {
        val text = """
            before
            <<<<<<< HEAD
            ours one
            ours two
            =======
            theirs one
            >>>>>>> side
            after
        """.trimIndent()

        val parse = parseConflictMarkers(text)

        assertFalse(parse.malformed)
        val conflict = parse.conflicts.single()
        assertEquals(0, conflict.id)
        assertEquals(1, conflict.startLine)
        assertEquals(6, conflict.endLine)
        assertEquals(listOf("ours one", "ours two"), conflict.ours)
        assertNull(conflict.base)
        assertEquals(listOf("theirs one"), conflict.theirs)
    }

    @Test
    fun `parses diff3 base section`() {
        val text = """
            <<<<<<< HEAD
            ours
            ||||||| base
            original
            =======
            theirs
            >>>>>>> side
        """.trimIndent()

        val conflict = parseConflictMarkers(text).conflicts.single()

        assertEquals(listOf("ours"), conflict.ours)
        assertEquals(listOf("original"), conflict.base)
        assertEquals(listOf("theirs"), conflict.theirs)
    }

    @Test
    fun `does not consume an incomplete conflict block`() {
        val text = """
            before
            <<<<<<< HEAD
            ours
            =======
            theirs
        """.trimIndent()

        val parse = parseConflictMarkers(text)

        assertTrue(parse.malformed)
        assertTrue(parse.conflicts.isEmpty())
    }

    @Test
    fun `applies selected lines and preserves other markers`() {
        val text = """
            before
            <<<<<<< HEAD
            ours one
            ours two
            =======
            theirs one
            >>>>>>> side
            between
            <<<<<<< HEAD
            second ours
            =======
            second theirs
            >>>>>>> side
            after
        """.trimIndent()
        val parse = parseConflictMarkers(text)
        val choice = MergeLineChoice(ours = setOf(1), theirs = setOf(0))

        val result = applyConflictChoices(text, mapOf(parse.conflicts[0].id to choice))

        assertEquals(
            """
                before
                ours two
                theirs one
                between
                <<<<<<< HEAD
                second ours
                =======
                second theirs
                >>>>>>> side
                after
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `takes all theirs while keeping context and line endings`() {
        val text = "a\r\n<<<<<<< HEAD\r\nours\r\n=======\r\ntheirs\r\n>>>>>>> side\r\nz\r\n"

        val result = applyConflictSide(text, MergeSide.Theirs)

        assertEquals("a\r\ntheirs\r\nz\r\n", result)
    }

    @Test
    fun `can resolve a conflict to an empty side`() {
        val text = "<<<<<<< HEAD\n=======\ntheirs\n>>>>>>> side\n"

        assertEquals("", applyConflictSide(text, MergeSide.Ours))
    }

    @Test
    fun `toggle and select all keep sides independent`() {
        val initial = MergeLineChoice()
            .toggle(MergeSide.Ours, 1)
            .toggle(MergeSide.Theirs, 0)

        assertEquals(setOf(1), initial.ours)
        assertEquals(setOf(0), initial.theirs)

        val allOurs = initial.selectAll(MergeSide.Ours, 3, 2)

        assertEquals(setOf(0, 1, 2), allOurs.ours)
        assertEquals(setOf(0), allOurs.theirs)
        assertTrue(allOurs.resolve(MergeConflict(0, 0, 3, listOf("a", "b", "c"), null, listOf("x", "y"))).isNotEmpty())
    }
}

private fun MergeLineChoice.resolve(conflict: MergeConflict): List<String> = conflict.resolve(this)
