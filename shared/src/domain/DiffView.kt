package org.baconana.mybeautifulgit_chan.domain

/** Как показывать изменения: одной колонкой или old/new рядом. */
enum class DiffLayout { Unified, SideBySide }

data class DiffViewOptions(
    val layout: DiffLayout = DiffLayout.Unified,
    val ignoreWhitespace: Boolean = false,
    val ignoreCase: Boolean = false,
)

/** Изменённый диапазон внутри [DiffLineView.text]. */
data class DiffSpan(val start: Int, val end: Int)

/**
 * Одна строка для отрисовки.
 *
 * [text] — содержимое без diff-префикса `+`, `-` или пробела. Для заголовков
 * файла, хунка и meta-строк сохраняется исходный текст. Смещения в [wordSpans]
 * и [syntaxSpans] относятся именно к [text].
 */
data class DiffLineView(
    val line: DiffLine,
    val text: String,
    val wordSpans: List<DiffSpan> = emptyList(),
    val syntaxSpans: List<SyntaxSpan> = emptyList(),
)

sealed interface DiffViewRow {
    /** Строка на всю ширину: file header, hunk header, meta или unified-строка. */
    data class Full(val view: DiffLineView) : DiffViewRow

    /** Пара old/new для side-by-side; отсутствующая сторона — null. */
    data class Split(
        val left: DiffLineView?,
        val right: DiffLineView?,
    ) : DiffViewRow

    /** Скрытые фильтром пары, различающиеся только пробелами/регистром. */
    data class Omitted(val count: Int) : DiffViewRow
}

/**
 * Собирает строки для unified или side-by-side отображения.
 *
 * Фильтры влияют только на отображение: исходный [ParsedDiff] и индексы строк
 * не меняются, поэтому stage/unstage/discard продолжают работать по тем же
 * lineIndex. Если пара `-old`/`+new` после нормализации совпадает и включён
 * соответствующий фильтр, пара скрывается, а вместо неё показывается
 * [DiffViewRow.Omitted] с количеством скрытых пар.
 */
fun buildDiffView(
    parsed: ParsedDiff,
    path: String?,
    options: DiffViewOptions = DiffViewOptions(),
): List<DiffViewRow> {
    val rows = ArrayList<DiffViewRow>()
    var index = 0

    while (index < parsed.lines.size) {
        val line = parsed.lines[index]
        val hunk = parsed.hunks.getOrNull(line.hunkIndex)

        if (line.kind == DiffLineKind.Hunk && hunk != null) {
            rows += DiffViewRow.Full(lineView(line, path))
            rows += hunkRows(hunk, path, options)

            index += 1
            while (index < parsed.lines.size && parsed.lines[index].hunkIndex == hunk.index) {
                index += 1
            }
        } else {
            rows += DiffViewRow.Full(lineView(line, path))
            index += 1
        }
    }

    return rows
}

private fun hunkRows(
    hunk: DiffHunk,
    path: String?,
    options: DiffViewOptions,
): List<DiffViewRow> {
    val rows = ArrayList<DiffViewRow>()
    var index = 0

    while (index < hunk.lines.size) {
        val line = hunk.lines[index]
        when (line.kind) {
            DiffLineKind.Context -> {
                val view = lineView(line, path)
                rows += if (options.layout == DiffLayout.SideBySide) {
                    DiffViewRow.Split(view, view)
                } else {
                    DiffViewRow.Full(view)
                }
                index += 1
            }

            DiffLineKind.Meta -> {
                rows += DiffViewRow.Full(lineView(line, path))
                index += 1
            }

            DiffLineKind.Added, DiffLineKind.Removed -> {
                val start = index
                while (
                    index < hunk.lines.size &&
                    (hunk.lines[index].kind == DiffLineKind.Added ||
                        hunk.lines[index].kind == DiffLineKind.Removed)
                ) {
                    index += 1
                }
                rows += changeRows(hunk.lines.subList(start, index), path, options)
            }

            DiffLineKind.Header, DiffLineKind.Hunk -> {
                rows += DiffViewRow.Full(lineView(line, path))
                index += 1
            }
        }
    }

    return rows
}

private data class ChangeUnit(
    val left: DiffLine?,
    val right: DiffLine?,
    val leftSpans: List<DiffSpan> = emptyList(),
    val rightSpans: List<DiffSpan> = emptyList(),
    val hidden: Boolean = false,
)

private fun changeRows(
    lines: List<DiffLine>,
    path: String?,
    options: DiffViewOptions,
): List<DiffViewRow> {
    val removed = lines.filter { it.kind == DiffLineKind.Removed }
    val added = lines.filter { it.kind == DiffLineKind.Added }
    val units = (0 until maxOf(removed.size, added.size)).map { pairIndex ->
        val left = removed.getOrNull(pairIndex)
        val right = added.getOrNull(pairIndex)
        val hidden = left != null && right != null && isIgnored(left, right, options)
        val spans = if (left != null && right != null && !hidden) {
            wordDiff(displayText(left), displayText(right), options)
        } else {
            null
        }
        ChangeUnit(
            left = left,
            right = right,
            leftSpans = spans?.first.orEmpty(),
            rightSpans = spans?.second.orEmpty(),
            hidden = hidden,
        )
    }

    val rows = ArrayList<DiffViewRow>()
    val hiddenCount = units.count { it.hidden }

    if (options.layout == DiffLayout.SideBySide) {
        units.filterNot { it.hidden }.forEach { unit ->
            rows += DiffViewRow.Split(
                left = unit.left?.let { lineView(it, path, unit.leftSpans) },
                right = unit.right?.let { lineView(it, path, unit.rightSpans) },
            )
        }
    } else {
        for (line in lines) {
            val unit = units.firstOrNull { it.left === line || it.right === line } ?: continue
            if (unit.hidden) continue
            val spans = when {
                unit.left === line -> unit.leftSpans
                unit.right === line -> unit.rightSpans
                else -> emptyList()
            }
            rows += DiffViewRow.Full(lineView(line, path, spans))
        }
    }

    if (hiddenCount > 0) {
        rows += DiffViewRow.Omitted(hiddenCount)
    }
    return rows
}

private fun lineView(
    line: DiffLine,
    path: String?,
    wordSpans: List<DiffSpan> = emptyList(),
): DiffLineView {
    val text = displayText(line)
    val syntax = if (line.kind == DiffLineKind.Header ||
        line.kind == DiffLineKind.Hunk ||
        line.kind == DiffLineKind.Meta
    ) {
        emptyList()
    } else {
        highlightSyntax(path, text)
    }
    return DiffLineView(
        line = line,
        text = text,
        wordSpans = wordSpans,
        syntaxSpans = syntax,
    )
}

/** Содержимое строки без маркера diff; табы раскрываются до расчёта spans. */
private fun displayText(line: DiffLine): String = when (line.kind) {
    DiffLineKind.Added, DiffLineKind.Removed, DiffLineKind.Context -> line.text.drop(1)
    DiffLineKind.Header, DiffLineKind.Hunk, DiffLineKind.Meta -> line.text
}.replace("\t", "    ")

private fun isIgnored(
    left: DiffLine,
    right: DiffLine,
    options: DiffViewOptions,
): Boolean {
    if (left.kind != DiffLineKind.Removed || right.kind != DiffLineKind.Added) return false
    if (!options.ignoreWhitespace && !options.ignoreCase) return false
    return normalize(displayText(left), options) == normalize(displayText(right), options)
}

private fun normalize(text: String, options: DiffViewOptions): String {
    var value = text
    if (options.ignoreWhitespace) value = value.filterNot { it.isWhitespace() }
    if (options.ignoreCase) value = value.lowercase()
    return value
}

private data class WordToken(
    val text: String,
    val start: Int,
    val end: Int,
    val whitespace: Boolean,
)

private fun tokenizeWords(text: String): List<WordToken> {
    val result = ArrayList<WordToken>()
    var index = 0
    while (index < text.length) {
        val start = index
        val char = text[index]
        if (char.isWhitespace()) {
            while (index < text.length && text[index].isWhitespace()) index += 1
            result += WordToken(text.substring(start, index), start, index, whitespace = true)
        } else if (char.isLetterOrDigit() || char == '_') {
            while (
                index < text.length &&
                (text[index].isLetterOrDigit() || text[index] == '_')
            ) {
                index += 1
            }
            result += WordToken(text.substring(start, index), start, index, whitespace = false)
        } else {
            index += 1
            result += WordToken(text.substring(start, index), start, index, whitespace = false)
        }
    }
    return result
}

/**
 * Возвращает изменённые диапазоны для старой и новой строки. Слова сравниваются
 * через LCS; при ignore-флагах пробелы и/или регистр не считаются изменением.
 */
private fun wordDiff(
    oldText: String,
    newText: String,
    options: DiffViewOptions,
): Pair<List<DiffSpan>, List<DiffSpan>> {
    val oldTokens = tokenizeWords(oldText).filterNot { options.ignoreWhitespace && it.whitespace }
    val newTokens = tokenizeWords(newText).filterNot { options.ignoreWhitespace && it.whitespace }
    val oldMatched = BooleanArray(oldTokens.size)
    val newMatched = BooleanArray(newTokens.size)

    val lcs = Array(oldTokens.size + 1) { IntArray(newTokens.size + 1) }
    for (oldIndex in oldTokens.indices.reversed()) {
        for (newIndex in newTokens.indices.reversed()) {
            lcs[oldIndex][newIndex] = if (
                sameWord(oldTokens[oldIndex], newTokens[newIndex], options)
            ) {
                lcs[oldIndex + 1][newIndex + 1] + 1
            } else {
                maxOf(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1])
            }
        }
    }

    var oldIndex = 0
    var newIndex = 0
    while (oldIndex < oldTokens.size && newIndex < newTokens.size) {
        if (sameWord(oldTokens[oldIndex], newTokens[newIndex], options)) {
            oldMatched[oldIndex] = true
            newMatched[newIndex] = true
            oldIndex += 1
            newIndex += 1
        } else if (lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) {
            oldIndex += 1
        } else {
            newIndex += 1
        }
    }

    return changedSpans(oldTokens, oldMatched) to changedSpans(newTokens, newMatched)
}

private fun sameWord(left: WordToken, right: WordToken, options: DiffViewOptions): Boolean {
    return if (options.ignoreCase) {
        left.text.equals(right.text, ignoreCase = true)
    } else {
        left.text == right.text
    }
}

private fun changedSpans(tokens: List<WordToken>, matched: BooleanArray): List<DiffSpan> {
    val spans = ArrayList<DiffSpan>()
    for (index in tokens.indices) {
        if (matched[index]) continue
        val token = tokens[index]
        val previous = spans.lastOrNull()
        if (previous != null && token.start <= previous.end) {
            spans[spans.lastIndex] = previous.copy(end = maxOf(previous.end, token.end))
        } else {
            spans += DiffSpan(token.start, token.end)
        }
    }
    return spans
}
