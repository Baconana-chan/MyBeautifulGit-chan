package org.baconana.mybeautifulgit_chan.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositorySessionStoreTest {

    @Test
    fun `missing file means an empty session`() {
        val file = Files.createTempDirectory("git-chan-session").resolve("session.properties")

        assertEquals(RepositorySession(), RepositorySessionStore(file).load())
    }

    @Test
    fun `round-trips tabs and the active repository`() {
        val dir = Files.createTempDirectory("git-chan-session")
        val file = dir.resolve("session.properties")
        val a = File(dir.toFile(), "a").path
        val b = File(dir.toFile(), "b").path
        val c = File(dir.toFile(), "c").path
        val store = RepositorySessionStore(file)

        store.save(listOf(a, b, c), b)

        val loaded = RepositorySessionStore(file).load()
        assertEquals(3, loaded.repositories.size)
        assertEquals(b, loaded.active)
        assertEquals(c, loaded.repositories[2])
    }

    @Test
    fun `deduplicates paths and falls back to the first active tab`() {
        val dir = Files.createTempDirectory("git-chan-session")
        val a = File(dir.toFile(), "a").path
        val b = File(dir.toFile(), "b").path
        val store = RepositorySessionStore(dir.resolve("session.properties"))

        store.save(listOf(a, a, b), File(dir.toFile(), "missing").path)

        val loaded = store.load()
        assertEquals(2, loaded.repositories.size)
        assertEquals(a, loaded.active)
    }

    @Test
    fun `respects the tab limit and clears active when there are no tabs`() {
        val dir = Files.createTempDirectory("git-chan-session")
        val a = File(dir.toFile(), "a").path
        val b = File(dir.toFile(), "b").path
        val store = RepositorySessionStore(dir.resolve("session.properties"), limit = 2)

        store.save(listOf(a, b, File(dir.toFile(), "c").path), a)
        assertEquals(2, store.load().repositories.size)

        store.save(emptyList(), a)
        val empty = store.load()
        assertTrue(empty.repositories.isEmpty())
        assertNull(empty.active)
    }
}