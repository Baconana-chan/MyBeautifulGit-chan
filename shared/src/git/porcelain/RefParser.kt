package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.RefKind

/**
 * Разбирает вывод:
 * `git for-each-ref --format=%(refname)%00%(objectname)%00%(objecttype)%00%(symref)%00%(upstream)%00%(upstream:track)%00%(HEAD) refs/heads refs/remotes refs/tags`.
 */
object RefParser {
    private val AHEAD = Regex("""ahead (\d+)""")
    private val BEHIND = Regex("""behind (\d+)""")

    fun parse(raw: String): List<GitRef> = raw.split('\n').mapNotNull { line ->
        val record = line.trimEnd('\r')
        if (record.isBlank()) return@mapNotNull null
        val fields = record.split('\u0000')
        if (fields.size < 7) return@mapNotNull null

        val name = fields[0]
        val kind = when {
            name.startsWith("refs/heads/") -> RefKind.LocalBranch
            name.startsWith("refs/remotes/") -> RefKind.RemoteBranch
            name.startsWith("refs/tags/") -> RefKind.Tag
            else -> RefKind.Other
        }
        val shortName = when (kind) {
            RefKind.LocalBranch -> name.removePrefix("refs/heads/")
            RefKind.RemoteBranch -> name.removePrefix("refs/remotes/")
            RefKind.Tag -> name.removePrefix("refs/tags/")
            RefKind.Other -> name
        }
        val track = fields[5]
        GitRef(
            name = name,
            shortName = shortName,
            target = fields[1],
            objectType = fields[2],
            kind = kind,
            isHead = fields[6].trim() == "*",
            isSymbolic = fields[3].isNotBlank(),
            symbolicTarget = fields[3].ifBlank { null },
            upstream = fields[4].ifBlank { null },
            ahead = AHEAD.find(track)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            behind = BEHIND.find(track)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            upstreamGone = track.contains("gone"),
        )
    }
}
