package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.GitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope

/**
 * Parser for:
 *
 * `git config --list --show-origin --show-scope --null --includes`
 *
 * The NUL-delimited stream is a sequence of (scope, origin, "key\nvalue")
 * triples. A value may contain newlines, so the key/value separator is only
 * the first newline in the payload.
 */
object ConfigParser {
    fun parse(output: String): List<GitConfigEntry> {
        if (output.isEmpty()) return emptyList()
        val fields = output.split('\u0000')
        val entries = ArrayList<GitConfigEntry>()
        var index = 0
        while (index + 2 < fields.size) {
            val scope = scope(fields[index])
            val origin = fields[index + 1]
            val payload = fields[index + 2]
            index += 3
            if (scope == null) continue

            val separator = payload.indexOf('\n')
            val key = if (separator >= 0) payload.substring(0, separator) else payload
            if (key.isBlank()) continue
            val value = if (separator >= 0) payload.substring(separator + 1) else ""
            entries += GitConfigEntry(
                key = key,
                value = value,
                scope = scope,
                origin = origin,
            )
        }
        return entries
    }

    private fun scope(value: String): GitConfigScope? = when (value.lowercase()) {
        "system" -> GitConfigScope.System
        "global" -> GitConfigScope.Global
        "local" -> GitConfigScope.Local
        "worktree" -> GitConfigScope.Worktree
        else -> null
    }
}
