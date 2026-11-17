package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.ReflogEntry

/** Разбирает `git reflog --all --format=%H%x00%gD%x00%gs%x00%ct`. */
object ReflogParser {
    fun parse(raw: String): List<ReflogEntry> = raw.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val fields = line.split('\u0000')
            if (fields.size < 4) return@mapNotNull null
            val time = fields[3].trim().toLongOrNull() ?: return@mapNotNull null
            ReflogEntry(
                hash = fields[0].trim(),
                selector = fields[1].trim(),
                subject = fields[2],
                time = time,
            )
        }
        .toList()
}
