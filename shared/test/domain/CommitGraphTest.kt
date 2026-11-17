package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitGraphTest {

    private fun commit(hash: String, vararg parents: String): Commit = Commit(
        hash = hash,
        parents = parents.toList(),
        authorName = "Test",
        authorEmail = "test@example.com",
        authorTime = 0,
        committerName = "Test",
        committerEmail = "test@example.com",
        committerTime = 0,
        subject = hash,
    )

    @Test
    fun `builds lanes for a merge and its side commit`() {
        val graph = buildCommitGraph(
            listOf(
                commit("merge", "main", "side"),
                commit("side", "base"),
                commit("main", "base"),
                commit("base"),
            ),
        )

        assertEquals(listOf("merge", "side", "main", "base"), graph.rows.map { it.commit.hash })
        assertEquals(0, graph.rows[0].lane)
        assertEquals(listOf(0, 1), graph.rows[0].parentLanes.toList())
        assertEquals(1, graph.rows[1].lane)
        assertEquals(listOf(1), graph.rows[1].parentLanes.toList())
        assertEquals(0, graph.rows[2].lane)
        assertEquals(2, graph.laneCount)
        assertTrue(graph.rows[0].commit.isMerge)
        assertEquals(1, graph.collapsedCount("merge"))
        assertEquals(1, graph.collapsedCount(graph.rows[0].commit.hash))
    }

    @Test
    fun `collapses only commits on the secondary side of a merge`() {
        val commits = listOf(
            commit("merge", "main", "side"),
            commit("side", "side-2"),
            commit("side-2", "base"),
            commit("main", "base"),
            commit("base"),
        )

        val collapsed = buildCommitGraph(commits, collapsedMerges = setOf("merge"))

        assertEquals(listOf("merge", "main", "base"), collapsed.rows.map { it.commit.hash })
        assertEquals(2, collapsed.collapsedCount("merge"))
        assertEquals(2, collapsed.collapsedCount(collapsed.rows.first().commit.hash))
    }

    @Test
    fun `keeps unrelated branches in separate lanes`() {
        val graph = buildCommitGraph(
            listOf(
                commit("merge", "main", "other"),
                commit("main", "base"),
                commit("other", "base"),
                commit("base"),
            ),
        )

        assertEquals(listOf(0, 0, 1, 0), graph.rows.map { it.lane })
        assertEquals(2, graph.laneCount)
    }
}
