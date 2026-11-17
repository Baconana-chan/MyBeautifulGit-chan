package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.Commit

/**
 * Разбирает вывод:
 * `git log --pretty=format:%H%x00%P%x00%an%x00%ae%x00%at%x00%cn%x00%ce%x00%ct%x00%s%x00%D`.
 *
 * Один коммит — одна строка, поля разделены NUL. `%s` не содержит переводов строк,
 * поэтому такой формат не ломается на многострочных сообщениях.
 */
object LogParser {
    fun parse(raw: String): List<Commit> = raw.split('\n').mapNotNull { line ->
        val record = line.trimEnd('\r')
        if (record.isBlank()) null else parseLine(record)
    }

    private fun parseLine(line: String): Commit? {
        val fields = line.split('\u0000')
        if (fields.size < 10) return null
        val parents = fields[1].split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val refs = fields[9].split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return Commit(
            hash = fields[0],
            parents = parents,
            authorName = fields[2],
            authorEmail = fields[3],
            authorTime = fields[4].toLongOrNull() ?: 0L,
            committerName = fields[5],
            committerEmail = fields[6],
            committerTime = fields[7].toLongOrNull() ?: 0L,
            subject = fields[8],
            refs = refs,
        )
    }
}
