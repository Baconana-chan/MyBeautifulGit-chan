package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiffViewTest {

    private val patch = """
        diff --git a/Main.kt b/Main.kt
        index 1111111..2222222 100644
        --- a/Main.kt
        +++ b/Main.kt
        @@ -1,1 +1,1 @@
        -val old = 1
        +val new = 1
    """.trimIndent()

    @Test
    fun `builds word spans for unified changed lines`() {
        val rows = buildDiffView(parsePatch(patch), "Main.kt")
        val removed = rows.filterIsInstance<DiffViewRow.Full>()
            .map { it.view }
            .single { it.line.kind == DiffLineKind.Removed }
        val added = rows.filterIsInstance<DiffViewRow.Full>()
            .map { it.view }
            .single { it.line.kind == DiffLineKind.Added }

        assertEquals("val old = 1", removed.text)
        assertEquals("val new = 1", added.text)
        assertTrue(removed.wordSpans.any { removed.text.substring(it.start, it.end) == "old" })
        assertTrue(added.wordSpans.any { added.text.substring(it.start, it.end) == "new" })
    }

    @Test
    fun `builds side by side rows with missing side`() {
        val rows = buildDiffView(
            parsed = parsePatch(
                """
                @@ -1,1 +1,1 @@
                -before
                +after
                +only right
                """.trimIndent(),
            ),
            path = "Main.kt",
            options = DiffViewOptions(layout = DiffLayout.SideBySide),
        )

        val splits = rows.filterIsInstance<DiffViewRow.Split>()
        assertEquals(2, splits.size)
        assertEquals("before", splits[0].left?.text)
        assertEquals("after", splits[0].right?.text)
        assertEquals(null, splits[1].left)
        assertEquals("only right", splits[1].right?.text)
    }

    @Test
    fun `hides pairs that differ only by ignored whitespace or case`() {
        val rows = buildDiffView(
            parsed = parsePatch(
                """
                @@ -1,2 +1,2 @@
                -Color
                +color
                -a b
                +a  b
                """.trimIndent(),
            ),
            path = "Main.kt",
            options = DiffViewOptions(ignoreWhitespace = true, ignoreCase = true),
        )

        val omitted = rows.filterIsInstance<DiffViewRow.Omitted>()
        assertEquals(1, omitted.size)
        assertEquals(2, omitted.single().count)
        assertTrue(rows.none { it is DiffViewRow.Full && it.view.line.kind == DiffLineKind.Removed })
        assertTrue(rows.none { it is DiffViewRow.Full && it.view.line.kind == DiffLineKind.Added })
    }

    @Test
    fun `keeps substantive changes when filters are enabled`() {
        val rows = buildDiffView(
            parsed = parsePatch(
                """
                @@ -1,2 +1,2 @@
                -Color
                +colour
                """.trimIndent(),
            ),
            path = "Main.kt",
            options = DiffViewOptions(ignoreWhitespace = true, ignoreCase = false),
        )

        assertTrue(rows.any { it is DiffViewRow.Full && it.view.line.kind == DiffLineKind.Removed })
        assertTrue(rows.any { it is DiffViewRow.Full && it.view.line.kind == DiffLineKind.Added })
        assertTrue(rows.none { it is DiffViewRow.Omitted })
    }

    @Test
    fun `highlights syntax in changed content`() {
        val rows = buildDiffView(parsePatch(patch), "Main.kt")
        val added = rows.filterIsInstance<DiffViewRow.Full>()
            .map { it.view }
            .single { it.line.kind == DiffLineKind.Added }

        assertTrue(added.syntaxSpans.any { span ->
            span.kind == SyntaxKind.Keyword &&
                added.text.substring(span.start, span.end) == "val"
        })
        assertTrue(added.syntaxSpans.any { span ->
            span.kind == SyntaxKind.Number &&
                added.text.substring(span.start, span.end) == "1"
        })
        assertNotNull(added.syntaxSpans.firstOrNull())
    }

    @Test
    fun `highlights common syntax tokens and markup`() {
        val kotlinText = "fun main() { val answer = 42 // life"
        val kotlin = highlightSyntax("Main.kt", kotlinText)
        assertTrue(kotlin.any { it.kind == SyntaxKind.Keyword && it.textIn(kotlinText) == "fun" })
        assertTrue(kotlin.any { it.kind == SyntaxKind.Function && it.textIn(kotlinText) == "main" })
        assertTrue(kotlin.any { it.kind == SyntaxKind.Number && it.textIn(kotlinText) == "42" })
        assertTrue(kotlin.any { it.kind == SyntaxKind.Comment && it.textIn(kotlinText) == "// life" })

        val markupText = "<a href=\"x\">link</a>"
        val markup = highlightSyntax("index.html", markupText)
        assertTrue(markup.any { it.kind == SyntaxKind.Tag && it.textIn(markupText) == "<a" })
        assertTrue(markup.any { it.kind == SyntaxKind.Attribute && it.textIn(markupText) == "href" })
        assertTrue(markup.any { it.kind == SyntaxKind.String && it.textIn(markupText) == "\"x\"" })
    }

    private fun SyntaxSpan.textIn(text: String): String = text.substring(start, end)
}
