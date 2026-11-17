package org.baconana.mybeautifulgit_chan.domain

/**
 * Один конфликтный блок из рабочего файла.
 *
 * [startLine] и [endLine] — индексы маркеров в разбитом по строкам тексте;
 * [base] есть только у diff3-конфликтов с маркером `|||||||`.
 */
data class MergeConflict(
    val id: Int,
    val startLine: Int,
    val endLine: Int,
    val ours: List<String>,
    val base: List<String>?,
    val theirs: List<String>,
)

data class MergeParse(
    val conflicts: List<MergeConflict>,
    val malformed: Boolean,
)

enum class MergeSide { Ours, Theirs }

/** Выбранные строки обеих сторон; порядок при сборке — ours, затем theirs. */
data class MergeLineChoice(
    val ours: Set<Int> = emptySet(),
    val theirs: Set<Int> = emptySet(),
) {
    val isEmpty: Boolean get() = ours.isEmpty() && theirs.isEmpty()

    fun isSelected(side: MergeSide, index: Int): Boolean = when (side) {
        MergeSide.Ours -> index in ours
        MergeSide.Theirs -> index in theirs
    }

    fun toggle(side: MergeSide, index: Int): MergeLineChoice = when (side) {
        MergeSide.Ours -> copy(ours = ours.toggle(index))
        MergeSide.Theirs -> copy(theirs = theirs.toggle(index))
    }

    fun selectAll(side: MergeSide, oursCount: Int, theirsCount: Int): MergeLineChoice = when (side) {
        MergeSide.Ours -> copy(ours = (0 until oursCount).toSet())
        MergeSide.Theirs -> copy(theirs = (0 until theirsCount).toSet())
    }
}

private fun Set<Int>.toggle(index: Int): Set<Int> =
    if (index in this) this - index else this + index

/**
 * Разбирает стандартные маркеры git. Блок без `=======` или `>>>>>>>`
 * помечается malformed и остаётся обычным текстом.
 */
fun parseConflictMarkers(text: String): MergeParse {
    val lines = splitMergeLines(text)
    val conflicts = mutableListOf<MergeConflict>()
    var malformed = false
    var index = 0

    while (index < lines.size) {
        if (!lines[index].startsWith("<<<<<<<")) {
            index++
            continue
        }

        val start = index
        val ours = mutableListOf<String>()
        var base: MutableList<String>? = null
        val theirs = mutableListOf<String>()
        var section = 0 // 0 = ours, 1 = base, 2 = theirs
        var sawSeparator = false
        var closed = false
        var cursor = index + 1

        while (cursor < lines.size) {
            val line = lines[cursor]
            when {
                section == 0 && line.startsWith("|||||||") -> {
                    base = mutableListOf()
                    section = 1
                }

                section != 2 && line == "=======" -> {
                    sawSeparator = true
                    section = 2
                }

                section == 2 && line.startsWith(">>>>>>>") -> {
                    closed = true
                    cursor++
                    break
                }

                else -> when (section) {
                    0 -> ours += line
                    1 -> base?.add(line)
                    else -> theirs += line
                }
            }
            cursor++
        }

        if (closed && sawSeparator) {
            conflicts += MergeConflict(
                id = conflicts.size,
                startLine = start,
                endLine = cursor - 1,
                ours = ours,
                base = base,
                theirs = theirs,
            )
            index = cursor
        } else {
            malformed = true
            index = start + 1
        }
    }

    return MergeParse(conflicts, malformed)
}

/**
 * Заменяет выбранные конфликты их разрешением, сохраняя остальной текст,
 * включая невыбранные маркеры.
 */
fun applyConflictChoices(
    text: String,
    choices: Map<Int, MergeLineChoice>,
): String {
    if (choices.isEmpty()) return text
    val parse = parseConflictMarkers(text)
    if (parse.conflicts.isEmpty()) return text

    val separator = mergeSeparator(text)
    val lines = splitMergeLines(text)
    val output = mutableListOf<String>()
    var cursor = 0

    for (conflict in parse.conflicts) {
        val choice = choices[conflict.id] ?: continue
        output += lines.subList(cursor, conflict.startLine)
        output += conflict.resolve(choice)
        cursor = conflict.endLine + 1
    }
    output += lines.subList(cursor, lines.size)
    return output.joinToString(separator)
}

/** «Взять всё наше/их»: заменяет каждый распознанный конфликт выбранной стороной. */
fun applyConflictSide(text: String, side: MergeSide): String {
    val conflicts = parseConflictMarkers(text).conflicts
    val choices = conflicts.associate { conflict ->
        conflict.id to MergeLineChoice(
            ours = if (side == MergeSide.Ours) conflict.ours.indices.toSet() else emptySet(),
            theirs = if (side == MergeSide.Theirs) conflict.theirs.indices.toSet() else emptySet(),
        )
    }
    return applyConflictChoices(text, choices)
}

/** Строки, которые попадут в файл вместо блока. */
fun MergeConflict.resolve(choice: MergeLineChoice): List<String> =
    choice.ours.sorted().mapNotNull { ours.getOrNull(it) } +
        choice.theirs.sorted().mapNotNull { theirs.getOrNull(it) }

private fun splitMergeLines(text: String): List<String> = text.split(mergeSeparator(text))

private fun mergeSeparator(text: String): String =
    if (text.contains("\r\n")) "\r\n" else "\n"
