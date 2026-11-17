package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.BranchCompareCommit
import org.baconana.mybeautifulgit_chan.domain.BranchCompareSide

/**
 * Разбирает вывод `git rev-list --left-right --count A...B`
 * и `git log --left-right --format=%H%x00%m%x00%s A...B`.
 */
object BranchCompareParser {
    fun parseCounts(raw: String): Pair<Int, Int> {
        val values = raw.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        if (values.size < 2) return 0 to 0
        return values[0] to values[1]
    }

    fun parseCommits(raw: String): List<BranchCompareCommit> =
        raw.split('\n').mapNotNull { line ->
            val record = line.trimEnd('\r')
            if (record.isBlank()) return@mapNotNull null
            val fields = record.split('\u0000')
            if (fields.size < 3) return@mapNotNull null
            val side = when (fields[1]) {
                "<" -> BranchCompareSide.First
                ">" -> BranchCompareSide.Second
                else -> return@mapNotNull null
            }
            BranchCompareCommit(
                hash = fields[0],
                subject = fields[2],
                side = side,
            )
        }
}