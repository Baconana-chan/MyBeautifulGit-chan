package org.baconana.mybeautifulgit_chan.domain

enum class DiffLineKind { Header, Hunk, Added, Removed, Context, Meta }

data class DiffLine(
    val text: String,
    val kind: DiffLineKind,
    /** Индекс хунка в [ParsedDiff.hunks]; -1 для заголовков файла. */
    val hunkIndex: Int = -1,
    /** Индекс строки внутри хунка; -1 для заголовка самого хунка. */
    val lineIndex: Int = -1,
)

data class DiffHunk(
    val index: Int,
    val fileHeader: List<String>,
    val header: String,
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val lines: List<DiffLine>,
) {
    /** Удаление файла нельзя применять частично: заголовок требует /dev/null. */
    val isDeletion: Boolean
        get() = fileHeader.any { it.startsWith("deleted file mode") }
}

data class ParsedDiff(
    val lines: List<DiffLine>,
    val hunks: List<DiffHunk>,
)

private val HUNK_RANGE = Regex("""^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@""")

/**
 * Разбирает unified diff в строки для отображения и хунки для частичного stage.
 * Заголовки файла сохраняются в каждом хунке, чтобы патч можно было применить
 * отдельно через `git apply --cached`.
 */
fun parsePatch(raw: String): ParsedDiff {
    val allLines = ArrayList<DiffLine>()
    val hunks = ArrayList<DiffHunk>()
    var fileHeader = ArrayList<String>()
    var currentHunk: DiffHunk? = null
    var currentHunkLines = ArrayList<DiffLine>()
    var nextLineIndex = 0

    fun finishHunk() {
        val hunk = currentHunk ?: return
        hunks[hunk.index] = hunk.copy(lines = currentHunkLines.toList())
        currentHunk = null
        currentHunkLines = ArrayList()
        nextLineIndex = 0
    }

    for (line in raw.lineSequence()) {
        val range = HUNK_RANGE.find(line)
        when {
            line.startsWith("diff --git ") -> {
                finishHunk()
                fileHeader = ArrayList()
                fileHeader += line
                allLines += DiffLine(line, DiffLineKind.Header)
            }

            range != null -> {
                finishHunk()
                val index = hunks.size
                val hunk = DiffHunk(
                    index = index,
                    fileHeader = fileHeader.toList(),
                    header = line,
                    oldStart = range.groupValues[1].toInt(),
                    oldCount = range.groupValues[2].toIntOrNull() ?: 1,
                    newStart = range.groupValues[3].toInt(),
                    newCount = range.groupValues[4].toIntOrNull() ?: 1,
                    lines = emptyList(),
                )
                hunks += hunk
                currentHunk = hunk
                allLines += DiffLine(line, DiffLineKind.Hunk, hunkIndex = index)
            }

            currentHunk != null -> {
                val kind = when {
                    line.startsWith("\\ No newline") -> DiffLineKind.Meta
                    line.startsWith("+") -> DiffLineKind.Added
                    line.startsWith("-") -> DiffLineKind.Removed
                    else -> DiffLineKind.Context
                }
                val hunkIndex = currentHunk!!.index
                val diffLine = DiffLine(
                    text = line,
                    kind = kind,
                    hunkIndex = hunkIndex,
                    lineIndex = nextLineIndex,
                )
                nextLineIndex += 1
                currentHunkLines += diffLine
                allLines += diffLine
            }

            else -> {
                fileHeader += line
                allLines += DiffLine(line, metaKind(line))
            }
        }
    }
    finishHunk()

    return ParsedDiff(lines = allLines, hunks = hunks)
}

/**
 * Классифицирует unified diff для отображения.
 */
fun parseDiff(raw: String): List<DiffLine> = parsePatch(raw).lines

/**
 * Собирает патч для одного хунка.
 *
 * [selectedLineIndexes] — индексы строк внутри [DiffHunk.lines]; `null` означает
 * весь хунк. Невыбранные добавления не попадают в патч, а невыбранные удаления
 * остаются контекстом (при stage) или исчезают (при unstage), поэтому патч
 * применяется к текущему index, а не к рабочему дереву.
 */
fun buildPartialPatch(
    hunk: DiffHunk,
    selectedLineIndexes: Set<Int>?,
    unstage: Boolean,
): String {
    val body = buildHunkBody(hunk, selectedLineIndexes, unstage) ?: return ""
    val oldStart = if (unstage) hunk.newStart else hunk.oldStart
    // У нового файла oldStart=0, а новая сторона начинается с 1.
    val newStart = when {
        unstage -> hunk.oldStart
        hunk.oldStart == 0 && hunk.oldCount == 0 -> hunk.newStart
        else -> hunk.oldStart
    }

    return buildString {
        hunk.fileHeader.forEach { appendLine(it) }
        appendLine("@@ -$oldStart,${body.oldCount} +$newStart,${body.newCount} @@")
        body.lines.forEach { appendLine(it) }
    }
}

/**
 * Собирает один патч из нескольких целых хунков одного файла.
 *
 * Для stage oldStart остаётся исходным, а newStart сдвигается только на
 * выбранные предыдущие хунки. Для unstage oldStart берётся из текущего index,
 * а целевой newStart сдвигается на оставшиеся (невыбранные) хунки.
 */
fun buildPartialPatch(
    diff: ParsedDiff,
    hunkIndexes: Set<Int>,
    unstage: Boolean,
): String = buildPartialPatch(
    diff = diff,
    hunkSelections = hunkIndexes.associateWith { null },
    unstage = unstage,
)

/**
 * Собирает патч из нескольких хунков и, при необходимости, отдельных строк
 * внутри них. Значение `null` означает весь хунк, пустой набор — ни одной
 * строки. Это нужно не только для stage, но и для discard: можно собрать
 * оставшиеся изменения, вернуть файл к index и применить только их.
 */
fun buildPartialPatch(
    diff: ParsedDiff,
    hunkSelections: Map<Int, Set<Int>?>,
    unstage: Boolean,
): String {
    if (hunkSelections.isEmpty()) return ""
    val selected = diff.hunks.filter { it.index in hunkSelections }
    if (selected.isEmpty()) return ""

    val result = StringBuilder()
    selected.first().fileHeader.forEach { result.appendLine(it) }

    var selectedDelta = 0
    var unselectedDelta = 0
    var emitted = false

    for (hunk in diff.hunks) {
        val body = if (hunk.index in hunkSelections) {
            buildHunkBody(
                hunk = hunk,
                selectedLineIndexes = hunkSelections[hunk.index],
                unstage = unstage,
            )
        } else {
            if (unstage) unselectedDelta += hunk.newCount - hunk.oldCount
            null
        }
        if (body == null) continue

        val oldStart = if (unstage) hunk.newStart else hunk.oldStart
        val newStart = if (unstage) {
            hunk.oldStart + unselectedDelta
        } else {
            val base = if (hunk.oldStart == 0 && hunk.oldCount == 0) {
                hunk.newStart
            } else {
                hunk.oldStart
            }
            base + selectedDelta
        }

        result.appendLine("@@ -$oldStart,${body.oldCount} +$newStart,${body.newCount} @@")
        body.lines.forEach { result.appendLine(it) }
        emitted = true

        if (!unstage) selectedDelta += body.newCount - body.oldCount
    }

    return if (emitted) result.toString() else ""
}

private data class HunkPatchBody(
    val lines: List<String>,
    val oldCount: Int,
    val newCount: Int,
)

private fun buildHunkBody(
    hunk: DiffHunk,
    selectedLineIndexes: Set<Int>?,
    unstage: Boolean,
): HunkPatchBody? {
    val hasSelection = hunk.lines.withIndex().any { (index, line) ->
        (line.kind == DiffLineKind.Added || line.kind == DiffLineKind.Removed) &&
            (selectedLineIndexes == null || index in selectedLineIndexes)
    }
    if (!hasSelection) return null

    val body = ArrayList<String>()
    var previousEmitted = false

    fun selected(index: Int) = selectedLineIndexes == null || index in selectedLineIndexes

    fun emit(line: String) {
        body += line
        previousEmitted = true
    }

    fun skip() {
        previousEmitted = false
    }

    for ((index, line) in hunk.lines.withIndex()) {
        when (line.kind) {
            DiffLineKind.Context -> emit(line.text)

            DiffLineKind.Added -> {
                val text = line.text.removePrefix("+")
                when {
                    unstage && selected(index) -> emit("-$text")
                    unstage -> emit(" $text")
                    selected(index) -> emit(line.text)
                    else -> skip()
                }
            }

            DiffLineKind.Removed -> {
                val text = line.text.removePrefix("-")
                when {
                    unstage && selected(index) -> emit("+$text")
                    unstage -> skip()
                    selected(index) -> emit(line.text)
                    else -> emit(" $text")
                }
            }

            DiffLineKind.Meta -> if (previousEmitted) emit(line.text)

            DiffLineKind.Header, DiffLineKind.Hunk -> Unit
        }
    }

    return HunkPatchBody(
        lines = body,
        oldCount = body.count { it.startsWith("-") || it.startsWith(" ") },
        newCount = body.count { it.startsWith("+") || it.startsWith(" ") },
    )
}

private fun metaKind(line: String): DiffLineKind = when {
    line.startsWith("index ") ||
        line.startsWith("--- ") ||
        line.startsWith("+++ ") ||
        line.startsWith("new file mode") ||
        line.startsWith("deleted file mode") ||
        line.startsWith("old mode") ||
        line.startsWith("new mode") ||
        line.startsWith("similarity index") ||
        line.startsWith("dissimilarity index") ||
        line.startsWith("rename from") ||
        line.startsWith("rename to") ||
        line.startsWith("Binary files") -> DiffLineKind.Meta

    else -> DiffLineKind.Context
}
