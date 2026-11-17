package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.CommitFileStat

/** Разбирает `git show --numstat --format=`; binary-файлы приходят как `- - path`. */
object CommitStatParser {
    fun parseNumstat(raw: String): List<CommitFileStat> = raw.lineSequence()
        .map { it.trimEnd('\r') }
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size < 3) return@mapNotNull null
            val additions = fields[0].toIntOrNull()
            val deletions = fields[1].toIntOrNull()
            CommitFileStat(
                path = fields.drop(2).joinToString("\t"),
                additions = additions,
                deletions = deletions,
                binary = additions == null || deletions == null,
            )
        }
        .toList()
}
