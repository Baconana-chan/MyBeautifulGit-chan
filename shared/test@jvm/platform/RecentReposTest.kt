package org.baconana.mybeautifulgit_chan.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentReposTest {

    @Test
    fun `missing file means no recent repositories`() {
        val file = Files.createTempDirectory("git-chan-recent").resolve("recent.properties")

        assertTrue(RecentRepos(file).load().isEmpty())
    }

    @Test
    fun `keeps newest unique paths and respects limit`() {
        val dir = Files.createTempDirectory("git-chan-recent")
        val file = dir.resolve("recent.properties")
        val repos = RecentRepos(file, limit = 3)
        val a = File(dir.toFile(), "a").path
        val b = File(dir.toFile(), "b").path
        val c = File(dir.toFile(), "c").path
        val d = File(dir.toFile(), "d").path

        repos.add(a)
        repos.add(b)
        repos.add(a)
        repos.add(c)
        repos.add(d)

        val loaded = repos.load()
        assertEquals(3, loaded.size)
        assertEquals(listOf(d, c, a), loaded)
        assertTrue(loaded.none { it == b })
    }

    @Test
    fun `remove drops only the requested path`() {
        val dir = Files.createTempDirectory("git-chan-recent")
        val repos = RecentRepos(dir.resolve("recent.properties"))
        val a = File(dir.toFile(), "a").path
        val b = File(dir.toFile(), "b").path

        repos.add(a)
        repos.add(b)
        val loaded = repos.remove(a)

        assertEquals(listOf(b), loaded)
        assertEquals(listOf(b), repos.load())
    }
}
