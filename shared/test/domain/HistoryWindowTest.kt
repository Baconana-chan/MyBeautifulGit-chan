package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryWindowTest {

    private fun commit(hash: String, parent: String? = null): Commit = Commit(
        hash = hash,
        parents = listOfNotNull(parent),
        authorName = "Test",
        authorEmail = "test@example.com",
        authorTime = 0,
        committerName = "Test",
        committerEmail = "test@example.com",
        committerTime = 0,
        subject = hash,
    )

    @Test
    fun `keeps already loaded tail when head is unchanged`() {
        val cached = listOf(commit("d", "c"), commit("c", "b"), commit("b", "a"), commit("a"))
        val fresh = cached.take(2)

        val window = mergeHistoryWindow(
            cached = cached,
            cachedHasMore = true,
            fresh = fresh,
            pageSize = 2,
        )

        assertEquals(listOf("d", "c", "b", "a"), window.commits.map { it.hash })
        assertTrue(window.reusedCached)
        assertTrue(window.hasMore)
    }

    @Test
    fun `discards stale tail after history reset`() {
        val cached = listOf(commit("d", "c"), commit("c", "b"), commit("b", "a"), commit("a"))
        val fresh = listOf(commit("reset", "a"), commit("a"))

        val window = mergeHistoryWindow(
            cached = cached,
            cachedHasMore = true,
            fresh = fresh,
            pageSize = 2,
        )

        assertEquals(listOf("reset", "a"), window.commits.map { it.hash })
        assertFalse(window.reusedCached)
        assertTrue(window.hasMore)
    }

    @Test
    fun `reuses ancestors after a new commit on the current branch`() {
        val cached = listOf(commit("b", "a"), commit("a"), commit("root"))
        val fresh = listOf(commit("c", "b"), commit("b", "a"), commit("a"), commit("root"))

        val window = mergeHistoryWindow(
            cached = cached,
            cachedHasMore = true,
            fresh = fresh,
            pageSize = 4,
            allowHeadAdvance = true,
        )

        assertEquals(listOf("c", "b", "a", "root"), window.commits.map { it.hash })
        assertTrue(window.reusedCached)
        assertTrue(window.hasMore)
    }

    @Test
    fun `does not reuse old all-branch tail when head moved`() {
        val cached = listOf(commit("old", "root"), commit("root"))
        val fresh = listOf(commit("new", "root"), commit("root"))

        val window = mergeHistoryWindow(
            cached = cached,
            cachedHasMore = true,
            fresh = fresh,
            pageSize = 2,
            allowHeadAdvance = false,
        )

        assertEquals(listOf("new", "root"), window.commits.map { it.hash })
        assertFalse(window.reusedCached)
    }

    @Test
    fun `short first page reports end of history`() {
        val window = mergeHistoryWindow(
            cached = emptyList(),
            cachedHasMore = true,
            fresh = listOf(commit("a")),
            pageSize = 200,
        )

        assertEquals(1, window.commits.size)
        assertFalse(window.hasMore)
        assertFalse(window.reusedCached)
    }

    @Test
    fun `empty fresh page clears a stale window`() {
        val window = mergeHistoryWindow(
            cached = listOf(commit("a")),
            cachedHasMore = false,
            fresh = emptyList(),
            pageSize = 200,
        )

        assertTrue(window.commits.isEmpty())
        assertFalse(window.hasMore)
    }
}
