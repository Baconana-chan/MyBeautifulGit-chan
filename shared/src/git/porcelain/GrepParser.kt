package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.GrepMatch

/**
 * Разбирает `git grep --null -n`: `path\0line\0text\n` для каждого совпадения.
 * NUL отделяет путь от номера строки и номер от текста, поэтому двоеточия
 * и пробелы в пути не меняют разбор. Текст одной строки не содержит перевода строки.
 */
object GrepParser {
    fun parse(raw: String): List<GrepMatch> {
        if (raw.isEmpty()) return emptyList()
        val fields = raw.split('\u0000')
        if (fields.size < 3) return emptyList()

        val result = mutableListOf<GrepMatch>()
        var path = fields[0]
        var index = 1
        while (index + 1 < fields.size && path.isNotEmpty()) {
            val lineNumber = fields[index].toIntOrNull() ?: break
            val payload = fields[index + 1]
            val newline = payload.indexOf('\n')
            val text = (if (newline >= 0) payload.substring(0, newline) else payload)
                .trimEnd('\r')
            result += GrepMatch(path = path, lineNumber = lineNumber, text = text)

            val nextPath = if (newline >= 0) payload.substring(newline + 1) else ""
            if (nextPath.isEmpty()) break
            path = nextPath
            index += 2
        }
        return result
    }
}
