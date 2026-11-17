package org.baconana.mybeautifulgit_chan.domain

enum class BisectMarkKind { Good, Bad, Skip }

data class BisectMark(
    val hash: String,
    val subject: String,
    val kind: BisectMarkKind,
) {
    val shortHash: String get() = hash.take(7)
}

/**
 * Снимок `git bisect`, собранный из BISECT_LOG/BISECT_START и текущего HEAD.
 * [firstBad] заполняется и после завершения bisect: git оставляет state до
 * `git bisect reset`, поэтому UI может показать найденный коммит.
 */
data class BisectState(
    val active: Boolean = false,
    val originalRef: String = "",
    val currentHash: String = "",
    val currentSubject: String = "",
    val good: List<BisectMark> = emptyList(),
    val bad: List<BisectMark> = emptyList(),
    val skipped: List<BisectMark> = emptyList(),
    val firstBad: BisectMark? = null,
    val log: String = "",
) {
    val complete: Boolean get() = firstBad != null

    val allMarks: List<BisectMark> get() = good + bad + skipped

    fun markFor(hash: String): BisectMark? =
        bad.firstOrNull { it.hash.equals(hash, ignoreCase = true) }
            ?: good.firstOrNull { it.hash.equals(hash, ignoreCase = true) }
            ?: skipped.firstOrNull { it.hash.equals(hash, ignoreCase = true) }
}

fun validBisectRef(value: String): Boolean {
    val clean = value.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        clean.none { it.isWhitespace() || it.isISOControl() }
}

fun bisectStartArgs(bad: String, good: String): List<String> =
    listOf("bisect", "start", bad.trim(), good.trim())

fun bisectMarkArgs(good: Boolean, hash: String? = null): List<String> = buildList {
    add("bisect")
    add(if (good) "good" else "bad")
    hash?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
}

fun bisectSkipArgs(): List<String> = listOf("bisect", "skip")

fun bisectResetArgs(): List<String> = listOf("bisect", "reset")

/**
 * Разбирает комментарии, которые git пишет в BISECT_LOG:
 * `# good: [hash] subject`, `# bad: ...`, `# skip: ...` и
 * `# first 'bad' commit: [hash] subject` (в старых версиях без кавычек).
 */
object BisectLogParser {

    private val markLine = Regex(
        """^#\s+(good|bad|skip):\s+\[([0-9a-fA-F]{4,64})]\s*(.*)$""",
    )
    private val firstBadLine = Regex(
        """^#\s+first\s+'?bad'?\s+commit:\s+\[([0-9a-fA-F]{4,64})]\s*(.*)$""",
    )

    fun parse(
        raw: String,
        active: Boolean,
        originalRef: String,
        currentHash: String,
        currentSubject: String,
    ): BisectState {
        val good = LinkedHashMap<String, BisectMark>()
        val bad = LinkedHashMap<String, BisectMark>()
        val skipped = LinkedHashMap<String, BisectMark>()
        var firstBad: BisectMark? = null

        raw.lineSequence().forEach { line ->
            firstBadLine.matchEntire(line.trim())?.let { match ->
                val hash = match.groupValues[1]
                val subject = match.groupValues[2].trim()
                firstBad = BisectMark(hash, subject, BisectMarkKind.Bad)
            } ?: markLine.matchEntire(line.trim())?.let { match ->
                val kind = when (match.groupValues[1]) {
                    "good" -> BisectMarkKind.Good
                    "bad" -> BisectMarkKind.Bad
                    else -> BisectMarkKind.Skip
                }
                val mark = BisectMark(
                    hash = match.groupValues[2],
                    subject = match.groupValues[3].trim(),
                    kind = kind,
                )
                when (kind) {
                    BisectMarkKind.Good -> good[mark.hash] = mark
                    BisectMarkKind.Bad -> bad[mark.hash] = mark
                    BisectMarkKind.Skip -> skipped[mark.hash] = mark
                }
            }
        }

        val current = currentHash.takeIf { it.isNotBlank() }
        val currentMark = current?.let { hash ->
            bad[hash] ?: good[hash] ?: skipped[hash]
        }
        val currentSubjectText = currentSubject.ifBlank {
            currentMark?.subject.orEmpty()
        }
        firstBad = firstBad?.let { found ->
            val known = bad[found.hash]
            BisectMark(
                hash = found.hash,
                subject = found.subject.ifBlank {
                    known?.subject ?: currentSubjectText
                },
                kind = BisectMarkKind.Bad,
            )
        }
        firstBad?.let { found -> bad[found.hash] = found }

        return BisectState(
            active = active,
            originalRef = originalRef.trim(),
            currentHash = current.orEmpty(),
            currentSubject = currentSubjectText,
            good = good.values.toList(),
            bad = bad.values.toList(),
            skipped = skipped.values.toList(),
            firstBad = firstBad,
            log = raw,
        )
    }
}
