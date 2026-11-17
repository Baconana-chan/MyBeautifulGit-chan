package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiffParserTest {
    @Test
    fun `classifies headers hunks and changes`() {
        val raw = """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,2 +1,2 @@
             same
            -old
            +new
        """.trimIndent()

        val lines = parseDiff(raw)

        assertEquals(
            listOf(
                DiffLineKind.Header,
                DiffLineKind.Meta,
                DiffLineKind.Meta,
                DiffLineKind.Meta,
                DiffLineKind.Hunk,
                DiffLineKind.Context,
                DiffLineKind.Removed,
                DiffLineKind.Added,
            ),
            lines.map { it.kind },
        )
    }

    @Test
    fun `parses hunk ranges and line indexes`() {
        val parsed = parsePatch(
            """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -2,3 +2,4 @@
             same
            -old
            +new
            +extra
             tail
        """.trimIndent(),
        )

        assertEquals(1, parsed.hunks.size)
        val hunk = parsed.hunks.single()
        assertEquals(2, hunk.oldStart)
        assertEquals(3, hunk.oldCount)
        assertEquals(2, hunk.newStart)
        assertEquals(4, hunk.newCount)
        assertEquals(listOf("same", "old", "new", "extra", "tail"), hunk.lines.map { it.text.drop(1) })
        assertEquals(listOf(0, 1, 2, 3, 4), hunk.lines.map { it.lineIndex })
        assertEquals(listOf(0, 0, 0, 0, 0), hunk.lines.map { it.hunkIndex })
    }

    @Test
    fun `builds a patch for one added line`() {
        val hunk = parsePatch(
            """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,3 +1,3 @@
             same
            -old
            +new
             end
        """.trimIndent(),
        ).hunks.single()

        val patch = buildPartialPatch(hunk, selectedLineIndexes = setOf(2), unstage = false)

        assertTrue(patch.contains("@@ -1,3 +1,4 @@"))
        assertTrue(patch.contains(" old"))
        assertTrue(patch.contains("+new"))
        assertFalse(patch.contains("-old"))
    }

    @Test
    fun `builds a reverse patch for one removed line`() {
        val hunk = parsePatch(
            """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,3 +1,3 @@
             same
            -old
            +new
             end
        """.trimIndent(),
        ).hunks.single()

        val stagePatch = buildPartialPatch(hunk, selectedLineIndexes = setOf(1), unstage = false)
        assertTrue(stagePatch.contains("@@ -1,3 +1,2 @@"))
        assertTrue(stagePatch.contains("-old"))
        assertFalse(stagePatch.contains("+new"))

        val unstagePatch = buildPartialPatch(hunk, selectedLineIndexes = setOf(1), unstage = true)
        assertTrue(unstagePatch.contains("@@ -1,3 +1,4 @@"))
        assertTrue(unstagePatch.contains("+old"))
        assertTrue(unstagePatch.contains(" new"))
    }

    @Test
    fun `builds one patch for multiple selected hunks`() {
        val diff = parsePatch(
            """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,2 +1,3 @@
             a
            -b
            +B
            +C
            @@ -10,2 +11,2 @@
             j
            -k
            +K
        """.trimIndent(),
        )

        val patch = buildPartialPatch(diff, setOf(0, 1), unstage = false)

        assertTrue(patch.contains("@@ -1,2 +1,3 @@"))
        assertTrue(patch.contains("@@ -10,2 +11,2 @@"))
        assertTrue(patch.contains("+C"))
        assertTrue(patch.contains("+K"))
        assertTrue(patch.contains("-b"))
        assertTrue(patch.contains("-k"))
    }

    @Test
    fun `builds multiple hunks with a target offset only for selected changes`() {
        val diff = parsePatch(
            """
            diff --git a/a.txt b/a.txt
            index 1111111..2222222 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,2 +1,3 @@
             a
            -b
            +B
            +C
            @@ -10,2 +11,2 @@
             j
            -k
            +K
        """.trimIndent(),
        )

        val patch = buildPartialPatch(diff, setOf(1), unstage = false)

        assertFalse(patch.contains("@@ -1,2 +1,3 @@"))
        assertTrue(patch.contains("@@ -10,2 +10,2 @@"))
        assertTrue(patch.contains("+K"))
        assertFalse(patch.contains("+C"))
    }

    @Test
    fun `keeps a removed line that starts with minus as a removal`() {
        val lines = parseDiff(
            """
            @@ -1 +1 @@
            --- old content
            +++ new content
            """.trimIndent(),
        )

        assertEquals(DiffLineKind.Hunk, lines[0].kind)
        assertEquals(DiffLineKind.Removed, lines[1].kind)
        assertEquals(DiffLineKind.Added, lines[2].kind)
    }
}
