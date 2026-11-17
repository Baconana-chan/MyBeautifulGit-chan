package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.BlameLine

/**
 * Разбирает `git blame --line-porcelain`.
 *
 * Porcelain повторяет метаданные для каждой строки. Заголовок блока:
 * `<hash> <original-line> <final-line> [group-size]`; текст идёт после таба.
 */
object BlameParser {
    private val header = Regex("^([0-9a-fA-F]{40,64}) \\d+ (\\d+)(?: \\d+)?$")

    fun parse(raw: String): List<BlameLine> {
        val result = mutableListOf<BlameLine>()
        var hash = ""
        var lineNumber = 0
        var authorName = ""
        var authorEmail = ""
        var authorTime = 0L
        var summary = ""
        var reading = false

        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trimEnd('\r')
            val headerMatch = header.matchEntire(line)
            if (headerMatch != null) {
                hash = headerMatch.groupValues[1]
                lineNumber = headerMatch.groupValues[2].toIntOrNull() ?: 0
                authorName = ""
                authorEmail = ""
                authorTime = 0L
                summary = ""
                reading = true
                continue
            }
            if (!reading) continue

            when {
                line.startsWith("author ") ->
                    authorName = line.removePrefix("author ")
                line.startsWith("author-mail ") ->
                    authorEmail = line.removePrefix("author-mail ").trim().removeSurrounding("<", ">")
                line.startsWith("author-time ") ->
                    authorTime = line.removePrefix("author-time ").trim().toLongOrNull() ?: 0L
                line.startsWith("summary ") ->
                    summary = line.removePrefix("summary ")
                line.startsWith("\t") -> {
                    if (lineNumber > 0) {
                        result += BlameLine(
                            hash = hash,
                            lineNumber = lineNumber,
                            authorName = authorName,
                            authorEmail = authorEmail,
                            authorTime = authorTime,
                            summary = summary,
                            text = line.removePrefix("\t"),
                        )
                    }
                    reading = false
                }
            }
        }
        return result
    }
}
