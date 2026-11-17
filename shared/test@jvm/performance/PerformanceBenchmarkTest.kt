package org.baconana.mybeautifulgit_chan.performance

import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.buildCommitGraph
import org.baconana.mybeautifulgit_chan.git.porcelain.LogParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Синтетические бенчмарки для кусков, которые не зависят от диска.
 *
 * Они не заменяют профиль на реальном репозитории на миллион коммитов, но
 * ловят алгоритмические регрессии: квадратичное построение графа, повторный
 * обход merge-веток на первом кадре и медленный парсер `git log`.
 *
 * Запуск:
 * ./kotlin.bat test -m shared -p jvm \
 *   --include-classes='org.baconana.mybeautifulgit_chan.performance.PerformanceBenchmarkTest'
 */
class PerformanceBenchmarkTest {

    private fun commit(hash: String, vararg parents: String): Commit = Commit(
        hash = hash,
        parents = parents.toList(),
        authorName = "Benchmark",
        authorEmail = "benchmark@example.com",
        authorTime = 0,
        committerName = "Benchmark",
        committerEmail = "benchmark@example.com",
        committerTime = 0,
        subject = "subject $hash",
    )

    @Test
    fun `linear graph with 100k commits`() {
        val commits = ArrayList<Commit>(100_000)
        for (index in 99_999 downTo 0) {
            val hash = "c${index.toString().padStart(6, '0')}"
            val parents = if (index == 0) {
                emptyArray()
            } else {
                arrayOf("c${(index - 1).toString().padStart(6, '0')}")
            }
            commits += commit(hash, *parents)
        }

        val started = System.nanoTime()
        val graph = buildCommitGraph(commits)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        println("[benchmark] linear graph: 100000 commits -> ${elapsedMs}ms, ${graph.rows.size} rows, ${graph.laneCount} lanes")
        assertEquals(100_000, graph.rows.size)
        assertTrue(elapsedMs < 20_000, "linear graph benchmark took ${elapsedMs}ms")
    }

    @Test
    fun `merge-heavy graph computes collapse sizes lazily`() {
        val chronological = ArrayList<Commit>(24_000)
        chronological += commit("root")
        var main = "root"
        for (index in 1..20_000) {
            if (index % 500 == 0) {
                val sideA = "side-a-$index"
                val sideB = "side-b-$index"
                val merge = "merge-$index"
                // Chronological order: parent side commit first, then merge.
                // Reversing the whole list gives a valid newest-first git log.
                chronological += commit(sideB, main)
                chronological += commit(sideA, sideB)
                chronological += commit(merge, main, sideA)
                main = merge
            } else {
                val hash = "m${index.toString().padStart(6, '0')}"
                chronological += commit(hash, main)
                main = hash
            }
        }
        val commits = chronological.asReversed()

        val started = System.nanoTime()
        val graph = buildCommitGraph(commits)
        val buildMs = (System.nanoTime() - started) / 1_000_000

        val countStarted = System.nanoTime()
        val counts = (1..10).map { graph.collapsedCount("merge-${it * 500}") }
        val countMs = (System.nanoTime() - countStarted) / 1_000_000

        println(
            "[benchmark] merge graph: ${commits.size} commits -> build=${buildMs}ms, " +
                "10 lazy collapse counts=${countMs}ms",
        )
        assertEquals(commits.size, graph.rows.size)
        assertTrue(counts.all { it == 2 }, "unexpected side-branch sizes: $counts")
        assertTrue(buildMs < 20_000, "merge graph benchmark took ${buildMs}ms")
    }

    @Test
    fun `log parser with 100k records`() {
        val count = 100_000
        val raw = buildString(count * 96) {
            repeat(count) { index ->
                val hash = "c${index.toString().padStart(6, '0')}"
                append(hash).append('\u0000')
                if (index > 0) append("c").append((index - 1).toString().padStart(6, '0'))
                append('\u0000')
                append("Benchmark\u0000benchmark@example.com\u00001700000000")
                append('\u0000')
                append("Benchmark\u0000benchmark@example.com\u00001700000000")
                append('\u0000')
                append("subject ").append(index).append('\u0000')
                append("HEAD -> main")
                append('\n')
            }
        }

        val started = System.nanoTime()
        val parsed = LogParser.parse(raw)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        println("[benchmark] log parser: $count records -> ${elapsedMs}ms")
        assertEquals(count, parsed.size)
        assertEquals("c000000", parsed.first().hash)
        assertTrue(elapsedMs < 20_000, "log parser benchmark took ${elapsedMs}ms")
    }
}
